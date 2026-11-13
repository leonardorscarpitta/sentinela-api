package example.bairrovivo.cidadao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import example.bairrovivo.privacidade.PiiMasker;

@RestController
@RequestMapping("/api/cidadaos")
public class CidadaoController {

    private static final Logger log = LoggerFactory.getLogger(CidadaoController.class);

    private final CidadaoRepository repository;

    public CidadaoController(CidadaoRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Long> cadastrar(@RequestBody Cidadao novo) {
        Cidadao salvo = repository.save(novo);
        String documento = salvo.getCpf();
        // LEAK (CPF -> log): the full CPF ends up in the application logs.
        log.info("Novo cadastro no Bairro Vivo, CPF {}", documento);
        // SAFE: the same data, masked before logging. Must not be reported.
        log.info("Cadastro confirmado para CPF {}", PiiMasker.cpf(salvo.getCpf()));
        return ResponseEntity.ok(salvo.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cidadao> buscar(@PathVariable Long id) {
        Cidadao cidadao = repository.findById(id).orElseThrow();
        // LEAK (entity with senha -> HTTP response): password, CPF and contacts are serialized to the client.
        return ResponseEntity.ok(cidadao);
    }
}
