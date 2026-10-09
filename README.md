# spring-aop-demo

A small, didactic Spring Boot project that demonstrates **Aspect-Oriented Programming (AOP)** with Spring AOP.

## What AOP is

Most code implements *business logic* (create an order, find an order…), but real applications also need *cross-cutting concerns*: logging, timing, auditing, exception handling. Without AOP that code gets copy-pasted into every method. With AOP you write the concern **once**, in an **aspect**, and declare **where** it applies with a **pointcut expression**.

- **Join point** — a point in the execution (in Spring AOP: a method execution).
- **Pointcut** — an expression selecting join points, e.g. `execution(* com.example..OrderService.*(..))` or `@annotation(LogExecutionTime)`.
- **Advice** — the code that runs at a matched join point: `@Before`, `@AfterReturning`, `@AfterThrowing`, `@Around`.
- **Aspect** — a class bundling pointcuts + advices (`@Aspect` + `@Component`).
- **Proxy** — Spring wraps matching beans in a proxy; the advice runs only when a call goes **through the proxy** (see Gotchas).

## Project structure

```
src/main/java/com/example/aopdemo/
├── AopDemoApplication.java        # @SpringBootApplication entry point
├── annotation/
│   ├── LogExecutionTime.java      # marker: "time this method"
│   └── Audited.java               # marker: "audit this method / whole class"
├── aspect/
│   ├── TimingAspect.java          # @Around + @annotation(LogExecutionTime): timing, entry/exit log
│   ├── ServiceLayerAspect.java    # @Before + within(order..*): service-layer entry log
│   ├── AuditAspect.java           # @Before/@AfterReturning/@AfterThrowing + @within/@annotation(Audited)
│   ├── ExceptionAspect.java       # @AfterThrowing + execution(order..*): exception logging
│   └── ExceptionTranslationAspect.java  # @Around + execution(...failOnDemand): exception translation
├── audit/
│   ├── AuditEntry.java            # record: scope, method, args, outcome, detail
│   └── AuditTrailStore.java       # thread-safe in-memory audit trail
├── timing/
│   └── TimingRecorder.java        # in-memory store of measured durations per method
├── order/
│   ├── Order.java                 # domain record
│   ├── OrderRepository.java       # in-memory repository
│   ├── OrderService.java          # business logic (create / get / failOnDemand / self-invocation demo)
│   ├── OrderNotFoundException.java
│   └── OrderProcessingException.java
└── web/
    ├── OrderController.java       # POST /orders, GET /orders/{id}, GET /orders/{id}/fail (@Audited class)
    ├── SelfInvocationDemoController.java  # GET /demo/self-invocation
    └── GlobalExceptionHandler.java        # exceptions -> HTTP status + JSON body
```

## How each aspect works

| Aspect | Advice | Pointcut | Effect |
|---|---|---|---|
| `TimingAspect` (`@Order(1)`, outermost) | `@Around` | `@annotation(LogExecutionTime)` | Logs entry with args, exit with result; records elapsed ns in `TimingRecorder` (in `finally`, so failures are timed too) |
| `ServiceLayerAspect` (`@Order(2)`) | `@Before` | `within(com.example.aopdemo.order..*)` | Debug log on entry of any domain/service method |
| `AuditAspect` (`@Order(3)`) | `@Before` | `@within(…Audited)` | `ENTERED` entry (scope `CLASS`) for every method of an `@Audited` class (`OrderController`) |
| `AuditAspect` | `@AfterReturning` | `@annotation(…Audited)` | `OK` entry with the return value for annotated service methods |
| `AuditAspect` | `@AfterThrowing` | `@annotation(…Audited)` | `FAILED` entry with exception + cause chain |
| `ExceptionAspect` (`@Order(4)`) | `@AfterThrowing` | `execution(* …order..*(..))` | Logs the escaping exception at ERROR |
| `ExceptionTranslationAspect` (`@Order(5)`, innermost) | `@Around` | `execution(* …OrderService.failOnDemand(..))` | Translates the raw `IllegalStateException` into `OrderProcessingException` |

Lower `@Order` value = higher precedence = **outermost** in the chain. The translation aspect is innermost, so every outer aspect observes the *translated* exception — the tests assert exactly that.

## Run it

Requirements: Java 17, Maven.

```bash
mvn clean verify        # build + all tests
mvn spring-boot:run     # start the app on http://localhost:8080
```

## Example calls

```bash
# Create an order
curl -s -X POST localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"product":"widget","quantity":2}'
# → 201 Created
# {"id":"3f9a…","product":"widget","quantity":2,"status":"CREATED"}

# Fetch it
curl -s localhost:8080/orders/3f9a…
# → 200 OK, the order JSON

# Missing order → 404
curl -s localhost:8080/orders/does-not-exist
# → 404 Not Found
# {"timestamp":"…","status":404,"error":"Not Found",
#  "message":"Order not found: does-not-exist"}

# Trigger a failure → raw IllegalStateException translated by the aspect → 500
curl -s localhost:8080/orders/123/fail
# → 500 Internal Server Error
# {"timestamp":"…","status":500,"error":"Internal Server Error",
#  "message":"Order storage failure: simulated storage failure for order 123"}

# Invalid input → 400
curl -s -X POST localhost:8080/orders \
  -H 'Content-Type: application/json' -d '{"product":"widget","quantity":0}'
# → 400 Bad Request, {"message":"quantity must be >= 1", …}

# Live proof of the self-invocation gotcha
curl -s localhost:8080/demo/self-invocation | python3 -m json.tool
# {
#   "callThroughProxy":            {"result": "traced-operation-result", "aspectApplied": true},
#   "selfInvocationThisDotCall":   {"result": "traced-operation-result", "aspectApplied": false,
#     "explanation": "callInternally() invokes traceOperation() on 'this', which bypasses
#                     the Spring proxy, so the aspect does not run"},
#   "selfInvocationViaInjectedProxy": {"result": "traced-operation-result", "aspectApplied": true,
#     "explanation": "callViaSelfProxy() invokes traceOperation() on the injected proxy,
#                     so the aspect runs"}
# }
```

What the server log shows for the `/fail` call (real output from a test run — outermost aspect first):

```
audit: entered audited scope OrderController#fail
>> ENTER OrderService#failOnDemand([123])
-- service layer entering OrderService#failOnDemand
translated IllegalStateException into OrderProcessingException
Unhandled exception in OrderService#failOnDemand: …OrderProcessingException: Order storage failure: …
audit: OrderService#failOnDemand failed with …OrderProcessingException: … (caused by java.lang.IllegalStateException: …)
~ OrderService#failOnDemand took 0.845886 ms
```

And for a timed call: `~ OrderService#traceOperation took 0.195394 ms`.

## Gotchas (with proofs in this repo)

1. **Self-invocation bypasses the proxy.** `OrderService.callInternally()` calls `this.traceOperation()`; no proxy is involved, so `@LogExecutionTime` is silently ignored — even though the method is annotated. Only external calls through the Spring bean (or through the `@Lazy`-injected self-proxy, see `callViaSelfProxy()`) trigger advice. Proven by `GET /demo/self-invocation` and `SelfInvocationAspectTest`.
2. **`@AfterThrowing` can only observe, never replace.** Exception *translation* requires `@Around` (catch + rethrow), as `ExceptionTranslationAspect` shows.
3. **Type names in pointcut strings must be fully qualified.** The expression is parsed by AspectJ at runtime, which does not see your `import`s. `@annotation(Audited)` silently matched nothing here; `@annotation(com.example.aopdemo.annotation.Audited)` works. (The `@annotation(logExecutionTime)` form in `TimingAspect` is different: it binds the annotation to an advice parameter, so no global name resolution is needed.)
4. **Advice order matters.** Multiple aspects form a chain ordered by `@Order`. Without it, precedence is undefined; put shared logic in documented orders (here: timing → logging → audit → exception-log → translation).
5. **JDK dynamic proxies vs CGLIB.** Spring Boot uses CGLIB class-based proxies by default, so concrete classes like `OrderService` (no interface) are proxied fine. With JDK dynamic proxies only interface methods could be advised, and `final` methods can never be advised by either.

## Key concepts

| Concept | Meaning | Example in this project |
|---|---|---|
| Join point | A method execution that *can* be intercepted | `OrderService.create(..)` |
| Pointcut | Expression selecting join points | `@annotation(LogExecutionTime)`, `execution(* …order..*(..))`, `within(…order..*)`, `@within(…Audited)` |
| Advice | Code run at the join point | `@Around measureExecutionTime`, `@Before`, `@AfterReturning`, `@AfterThrowing` |
| Aspect | Advice + pointcuts in one bean | `TimingAspect`, `AuditAspect`, … |
| Proxy | Wrapper that runs the advice chain | CGLIB subclass of `OrderService` |
| Weaving | Applying aspects to beans (at startup, via proxies) | `AnnotationAwareAspectJAutoProxyCreator` |
| `@Order` | Precedence between aspects (lower = outermost) | `TimingAspect @Order(1)` … `ExceptionTranslationAspect @Order(5)` |

## Tests

`mvn clean verify` — 15 tests, all green:

| Test class | What it asserts |
|---|---|
| `TimingAspectTest` (3) | Duration recorded per call; recorded even on failure; nothing recorded for unannotated methods |
| `AuditAspectTest` (2) | `OK` entry with args+result after success; `FAILED` entry with translated exception after failure |
| `ExceptionAspectTest` (2) | `@AfterThrowing` logged the *translated* `OrderProcessingException` and the plain `OrderNotFoundException` (via Logback `ListAppender`) |
| `SelfInvocationAspectTest` (3) | Aspect runs through the proxy; bypassed by `this.` self-call; runs again via injected self-proxy |
| `OrderControllerTest` (5) | 201/400/404/500 over HTTP, class-level `@within` audit entry, translated-exception audit detail, self-invocation endpoint booleans |

## License

MIT — see [LICENSE](LICENSE).
