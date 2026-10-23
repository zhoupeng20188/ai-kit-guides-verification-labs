package guides.bootlab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SecurityContractTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    static ResultHandler observed(String name) {
        return result -> System.out.println("OBSERVATION " + name + "=" + result.getResponse().getStatus());
    }

    @Test void authenticatedGetSucceeds() throws Exception {
        mvc.perform(get("/notes").with(user("alice"))).andExpect(status().isOk())
                .andDo(observed("authenticated_get"));
    }
    @Test void anonymousGetRequiresAuthentication() throws Exception {
        mvc.perform(get("/notes")).andExpect(status().isUnauthorized()).andDo(observed("anonymous_get"));
    }
    @Test void authenticatedPostWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/notes").with(user("alice")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"demo\"}")).andExpect(status().isForbidden())
                .andDo(observed("authenticated_post_without_csrf"));
    }
    @Test void authenticatedPostWithCsrfSucceeds() throws Exception {
        mvc.perform(post("/notes").with(user("alice")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"demo\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").isNumber()).andDo(observed("authenticated_post_with_csrf"));
    }
    @Test void invalidCsrfIsForbidden() throws Exception {
        mvc.perform(post("/notes").with(user("alice")).with(csrf().useInvalidToken())
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"demo\"}"))
                .andExpect(status().isForbidden()).andDo(observed("invalid_csrf"));
    }
    @Test void unsecuredHarnessAcceptsAPostTheApplicationRejects() throws Exception {
        MockMvc unsecured = MockMvcBuilders.standaloneSetup(new NotesController()).build();
        unsecured.perform(post("/notes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"demo\"}")).andExpect(status().isOk())
                .andDo(observed("unsecured_harness_post"));
    }
}
