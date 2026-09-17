package br.com.seuprojeto.api;

import br.com.seuprojeto.ia.IaNaoConfiguradaException;
import br.com.seuprojeto.ia.ImagemInvalidaException;
import br.com.seuprojeto.ia.LeituraDeBilheteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduz as falhas da leitura de bilhete em status HTTP.
 *
 * <p>A separacao importa para quem consome: 400 e "sua imagem", 503 e "falta chave
 * aqui no servidor" e 502 e "a API externa falhou" - cada um pede uma acao diferente.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(ImagemInvalidaException.class)
    public ProblemDetail imagemInvalida(ImagemInvalidaException e) {
        return detalhe(HttpStatus.BAD_REQUEST, "Imagem invalida", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail arquivoGrandeDemais(MaxUploadSizeExceededException e) {
        return detalhe(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo grande demais",
                "o upload excedeu o limite configurado");
    }

    @ExceptionHandler(IaNaoConfiguradaException.class)
    public ProblemDetail iaNaoConfigurada(IaNaoConfiguradaException e) {
        return detalhe(HttpStatus.SERVICE_UNAVAILABLE, "Leitura por IA indisponivel", e.getMessage());
    }

    @ExceptionHandler(LeituraDeBilheteException.class)
    public ProblemDetail falhaNaLeitura(LeituraDeBilheteException e) {
        return detalhe(HttpStatus.BAD_GATEWAY, "Falha ao ler o bilhete", e.getMessage());
    }

    private static ProblemDetail detalhe(HttpStatus status, String titulo, String mensagem) {
        ProblemDetail problema = ProblemDetail.forStatus(status);
        problema.setTitle(titulo);
        problema.setDetail(mensagem);
        return problema;
    }
}
