package br.com.seuprojeto.dominio;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizacao de nome de clube, para casar grafias diferentes da mesma equipe.
 *
 * <p>Fontes discordam o tempo todo: a API de resultados manda "Atletico Mineiro",
 * a de odds manda "Atletico-MG" e o bilhete impresso manda "Galo". Aqui resolvemos
 * so a parte deterministica; apelidos ficam em {@code apelidos_de_time}.
 *
 * <p>Nao removemos sufixo de estado de proposito. Tirar o "-MG" faria "America-MG" e
 * "America-RN" colidirem no mesmo texto, e um vinculo errado e pior que nenhum: ele
 * contamina o EV silenciosamente. Quem nao casa aqui volta como nao resolvido.
 */
public final class NomeDeTime {

    /** Ruido societario que aparece no nome formal e nunca no uso corrente. */
    private static final Set<String> TOKENS_DESCARTAVEIS = Set.of(
            "fc", "ec", "sc", "ac", "cr", "aa", "ca", "se", "ed",
            "futebol", "clube", "club", "esporte", "esportivo", "esportiva",
            "associacao", "atletica", "sociedade", "regatas", "recreativo",
            "de", "do", "da", "dos", "das", "e");

    private NomeDeTime() {
    }

    /**
     * Forma comparavel do nome: sem acento, minusculo, sem pontuacao e com
     * espacos colapsados. "Atletico-MG" vira "atletico mg".
     */
    public static String normalizar(String nome) {
        if (nome == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Chave de casamento: o normalizado sem o ruido societario.
     * "Clube de Regatas do Flamengo" e "Flamengo" caem os dois em "flamengo".
     *
     * <p>Se a limpeza esvaziar o nome - caso de clubes cujo nome util e justamente
     * um desses tokens - devolvemos o normalizado inteiro em vez de string vazia.
     */
    public static String chave(String nome) {
        String normalizado = normalizar(nome);
        if (normalizado.isEmpty()) {
            return "";
        }
        List<String> restantes = Arrays.stream(normalizado.split(" "))
                .filter(token -> !TOKENS_DESCARTAVEIS.contains(token))
                .toList();
        return restantes.isEmpty() ? normalizado : String.join(" ", restantes);
    }
}
