package com.autobots.automanager.controles;

import com.autobots.automanager.dto.Dados.EnderecoDados;
import com.autobots.automanager.dto.Respostas.EnderecoResposta;
import com.autobots.automanager.modelo.AdicionadorLinkEndereco;
import com.autobots.automanager.servicos.EnderecoServico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes/{clienteId}/endereco")
public class EnderecoControle {
    private final EnderecoServico servico;
    private final AdicionadorLinkEndereco links;
    public EnderecoControle(EnderecoServico servico, AdicionadorLinkEndereco links) { this.servico = servico; this.links = links; }
    @GetMapping
    public EntityModel<EnderecoResposta> obter(@PathVariable @Positive long clienteId) {
        return links.adicionarLink(clienteId, servico.obter(clienteId));
    }
    @PostMapping
    public ResponseEntity<EntityModel<EnderecoResposta>> cadastrar(@PathVariable @Positive long clienteId,
            @RequestBody @Valid EnderecoDados dados) {
        var modelo = links.adicionarLink(clienteId, servico.cadastrar(clienteId, dados));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }
    @PutMapping
    public EntityModel<EnderecoResposta> atualizar(@PathVariable @Positive long clienteId, @RequestBody @Valid EnderecoDados dados) {
        return links.adicionarLink(clienteId, servico.atualizar(clienteId, dados));
    }
    @DeleteMapping
    public ResponseEntity<Void> excluir(@PathVariable @Positive long clienteId) {
        servico.excluir(clienteId);
        return ResponseEntity.noContent().build();
    }
}
