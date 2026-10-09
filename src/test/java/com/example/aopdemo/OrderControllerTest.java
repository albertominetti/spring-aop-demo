package com.example.aopdemo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.aopdemo.audit.AuditTrailStore;
import com.example.aopdemo.order.Order;
import com.example.aopdemo.order.OrderRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * End-to-end test: the aspects wrap real controller/service calls and the
 * class-level ({@code @within}) audit entries show up as well.
 *
 * <p>Note: MockMvc is built manually from the shared {@code WebApplicationContext}
 * instead of {@code @AutoConfigureMockMvc} so this class reuses the <i>same</i>
 * Spring context as every other test. That matters because aspects resolve
 * collaborators through the static {@code BeanLookup}, which always points at
 * the most recently booted context — a second context would silently divert
 * all audit/timing records away from the stores under test.
 */
@SpringBootTest
class OrderControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void buildMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditTrailStore auditTrailStore;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("POST /orders creates an order and the controller is audited via @within")
    void createOrder() throws Exception {
        String body = """
                {"product":"widget","quantity":2}
                """;

        String response = mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.product").value("widget"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();

        Order created = objectMapper.readValue(response, Order.class);
        assertThat(orderRepository.findById(created.id())).isPresent();

        // @within(Audited) on the controller class -> CLASS-scope entry
        assertThat(auditTrailStore.entries()).anySatisfy(entry -> {
            assertThat(entry.scope()).isEqualTo("CLASS");
            assertThat(entry.method()).isEqualTo("OrderController#create");
            assertThat(entry.outcome()).isEqualTo("ENTERED");
        });
    }

    @Test
    @DisplayName("POST /orders with an invalid quantity returns 400")
    void createOrderValidation() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product\":\"widget\",\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("quantity must be >= 1"));
    }

    @Test
    @DisplayName("GET /orders/{id} returns the order, or 404 when missing")
    void getOrder() throws Exception {
        mockMvc.perform(get("/orders/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: does-not-exist"));
    }

    @Test
    @DisplayName("GET /orders/{id}/fail returns 500 with the exception translated by the aspect")
    void failOnDemand() throws Exception {
        mockMvc.perform(get("/orders/123/fail"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Order storage failure: simulated storage failure for order 123"));

        // @AfterThrowing recorded the *translated* exception (translation aspect
        // has lower precedence, i.e. sits closer to the target method)
        assertThat(auditTrailStore.entries()).anySatisfy(entry -> {
            assertThat(entry.method()).isEqualTo("OrderService#failOnDemand");
            assertThat(entry.outcome()).isEqualTo("FAILED");
            assertThat(entry.detail()).contains("OrderProcessingException")
                    .contains("IllegalStateException"); // the original cause
        });
    }

    @Test
    @DisplayName("GET /demo/self-invocation reports proxy bypass (or weaving, in CTW mode)")
    void selfInvocationDemo() throws Exception {
        mockMvc.perform(get("/demo/self-invocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.callThroughProxy.aspectApplied").value(true))
                .andExpect(jsonPath("$.selfInvocationThisDotCall.aspectApplied").value(compileTimeWeaving()))
                .andExpect(jsonPath("$.selfInvocationViaInjectedProxy.aspectApplied").value(true));
    }

    /**
     * {@code true} only under the {@code aspectj-ctw} profile, which sets
     * {@code spring.aop.auto=false} as a system property for the test JVM.
     * With compile-time weaving there is no proxy to bypass, so self-calls are advised.
     */
    private static boolean compileTimeWeaving() {
        return "false".equals(System.getProperty("spring.aop.auto"));
    }
}
