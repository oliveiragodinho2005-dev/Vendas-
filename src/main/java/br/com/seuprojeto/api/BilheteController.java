package br.com.seuprojeto.api;

import br.com.seuprojeto.ia.ImagemDeBilhete;
import br.com.seuprojeto.ia.ImagemInvalidaException;
import br.com.seuprojeto.ia.claude.PropriedadesIa;
import br.com.seuprojeto.servico.AnaliseDeBilhete;
import br.com.seuprojeto.servico.AnalisadorDeBilhete;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload do print do bilhete e devolucao das selecoes lidas. */
@RestController
@RequestMapping("/api/bilhetes")
public class BilheteController {

    private final AnalisadorDeBilhete analisador;
    private final PropriedadesIa propriedades;

    public BilheteController(AnalisadorDeBilhete analisador, PropriedadesIa propriedades) {
        this.analisador = analisador;
        this.propriedades = propriedades;
    }

    @PostMapping(path = "/analise", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnaliseDeBilhete analisar(@RequestParam("imagem") MultipartFile imagem) {
        if (imagem.isEmpty()) {
            throw new ImagemInvalidaException("nenhuma imagem enviada no campo 'imagem'");
        }
        byte[] conteudo;
        try {
            conteudo = imagem.getBytes();
        } catch (IOException e) {
            throw new ImagemInvalidaException("nao foi possivel ler o arquivo enviado: " + e.getMessage());
        }
        return analisador.analisar(
                ImagemDeBilhete.de(conteudo, imagem.getContentType(), propriedades.limiteImagemEmBytes()));
    }
}
