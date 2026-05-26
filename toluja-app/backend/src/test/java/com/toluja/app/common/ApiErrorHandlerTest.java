package com.toluja.app.common;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ApiErrorHandlerTest {

    @Test
    void shouldReturnJsonErrorWhenResponseAlreadyHasGzipContentType() throws Exception {
        standaloneSetup(new GzipFailingController())
                .setControllerAdvice(new ApiErrorHandler())
                .build()
                .perform(get("/download"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.message", is("template Linux do print agent nao encontrado")));
    }

    @Controller
    static class GzipFailingController {

        @GetMapping("/download")
        void download(HttpServletResponse response) {
            response.setContentType("application/gzip");
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "template Linux do print agent nao encontrado"
            );
        }
    }
}
