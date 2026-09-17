package br.com.seuprojeto.ia;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Imagem de um bilhete pronta para envio ao modelo de visao.
 *
 * <p>Valida o que a API aceita antes de gastar uma chamada: tipo de midia
 * suportado, conteudo nao vazio e tamanho dentro do limite.
 */
public record ImagemDeBilhete(byte[] conteudo, String mediaType) {

    public static final Set<String> TIPOS_SUPORTADOS =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    /** Apelidos comuns que navegadores e celulares enviam no lugar do tipo canonico. */
    private static final Map<String, String> SINONIMOS = Map.of(
            "image/jpg", "image/jpeg",
            "image/pjpeg", "image/jpeg",
            "image/x-png", "image/png");

    public ImagemDeBilhete {
        Objects.requireNonNull(conteudo, "conteudo");
        Objects.requireNonNull(mediaType, "mediaType");
        if (conteudo.length == 0) {
            throw new ImagemInvalidaException("a imagem esta vazia");
        }
        if (!TIPOS_SUPORTADOS.contains(mediaType)) {
            throw new ImagemInvalidaException(
                    "tipo de imagem nao suportado: " + mediaType + "; use " + TIPOS_SUPORTADOS);
        }
    }

    /**
     * Constroi a imagem normalizando o tipo declarado pelo cliente e conferindo o limite
     * de tamanho, que existe porque a API rejeita imagens grandes e a chamada seria perdida.
     */
    public static ImagemDeBilhete de(byte[] conteudo, String mediaTypeDeclarado, long limiteEmBytes) {
        if (conteudo == null || conteudo.length == 0) {
            throw new ImagemInvalidaException("a imagem esta vazia");
        }
        if (limiteEmBytes > 0 && conteudo.length > limiteEmBytes) {
            throw new ImagemInvalidaException(
                    "imagem de %d bytes excede o limite de %d bytes".formatted(conteudo.length, limiteEmBytes));
        }
        return new ImagemDeBilhete(conteudo, normalizarTipo(mediaTypeDeclarado));
    }

    private static String normalizarTipo(String mediaTypeDeclarado) {
        if (mediaTypeDeclarado == null || mediaTypeDeclarado.isBlank()) {
            throw new ImagemInvalidaException("tipo de imagem nao informado");
        }
        // Descarta parametros como "; charset=..." que alguns clientes anexam.
        String limpo = mediaTypeDeclarado.split(";")[0].trim().toLowerCase(Locale.ROOT);
        return SINONIMOS.getOrDefault(limpo, limpo);
    }

    public int tamanhoEmBytes() {
        return conteudo.length;
    }
}
