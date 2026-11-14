package example.bairrovivo.saude;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TriagemService {

    private final RestTemplate http = new RestTemplate();

    public void registrar(Triagem triagem) throws IOException {
        Map<String, Object> evento = Map.of(
                "cid", triagem.getCid10(),
                "sintomas", triagem.getSintomas(),
                "prioridade", triagem.getPrioridade());
        // LEAK (CID-10 / sintomas -> external API): health data sent to a third-party analytics partner over plain HTTP.
        http.postForObject("http://analytics.parceiro.example/v1/eventos", evento, Void.class);

        String linha = triagem.getCns() + ";" + triagem.getPrioridade() + "\n";
        // LEAK (CNS -> file): the Cartão SUS number is written to a world-readable temporary file.
        Files.writeString(Path.of("/tmp/exportacao-triagem.csv"), linha, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
