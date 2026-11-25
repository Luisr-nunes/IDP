package br.com.mentoria.validacao.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FrontEndTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void raizAbreATela() throws Exception {
        mvc.perform(get("/")).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"validar\"")));
        mvc.perform(get("/app.js")).andExpect(status().isOk());
        mvc.perform(get("/app.css")).andExpect(status().isOk());
        mvc.perform(get("/banco.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"baixar-tudo\"")));
        mvc.perform(get("/banco.js")).andExpect(status().isOk());
    }

    @Test
    void telasNaoInterpretamHtmlComDadosDaApi() throws IOException {
        for (String arquivo : new String[]{"static/app.js", "static/banco.js"}) {
            String js = new ClassPathResource(arquivo).getContentAsString(StandardCharsets.UTF_8);
            assertFalse(js.contains(".innerHTML") || js.contains(".outerHTML") || js.contains("insertAdjacentHTML")
                    || js.contains("createContextualFragment") || js.contains("document.write"), arquivo);
        }
    }
}
