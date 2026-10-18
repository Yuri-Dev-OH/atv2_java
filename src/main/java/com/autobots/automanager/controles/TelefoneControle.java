package com.autobots.automanager.controles;

import com.autobots.automanager.dto.Dados.TelefoneDados;
import com.autobots.automanager.dto.Respostas.TelefoneResposta;
import com.autobots.automanager.modelo.AdicionadorLinkTelefone;
import com.autobots.automanager.servicos.TelefoneServico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes/{clienteId}/telefones")
public class TelefoneControle {
    private final TelefoneServico servico;
    private final AdicionadorLinkTelefone links;
    public TelefoneControle(TelefoneServico servico, AdicionadorLinkTelefone links) { this.servico = servico; this.links = links; }
    @GetMapping
    public CollectionModel<EntityModel<TelefoneResposta>> listar(@PathVariable @Positive long clienteId) {
        return links.adicionarLink(clienteId, servico.listar(clienteId));
    }
    @GetMapping("/{id}")
    public EntityModel<TelefoneResposta> obter(@PathVariable @Positive long clienteId, @PathVariable @Positive long id) {
        return links.adicionarLink(clienteId, servico.obter(clienteId, id));
    }
    @PostMapping
    public ResponseEntity<EntityModel<TelefoneResposta>> cadastrar(@PathVariable @Positive long clienteId,
            @RequestBody @Valid TelefoneDados dados) {
        var modelo = links.adicionarLink(clienteId, servico.cadastrar(clienteId, dados));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }
    @PutMapping("/{id}")
    public EntityModel<TelefoneResposta> atualizar(@PathVariable @Positive long clienteId, @PathVariable @Positive long id,
            @RequestBody @Valid TelefoneDados dados) {
        return links.adicionarLink(clienteId, servico.atualizar(clienteId, id, dados));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable @Positive long clienteId, @PathVariable @Positive long id) {
        servico.excluir(clienteId, id);
        return ResponseEntity.noContent().build();
    }
}
