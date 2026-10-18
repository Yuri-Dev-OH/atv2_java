package com.autobots.automanager.configuracao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.MediaTypeConfigurationCustomizer;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.util.ClassUtils;

/**
 * Habilita os formatos hipermídia da API.
 *
 * <p>HAL ({@code application/hal+json}, também servido como {@code application/json}) entrega os links. HAL-FORMS
 * ({@code application/prs.hal-forms+json}) acrescenta {@code _templates} com método HTTP e campos de cada ação.
 */
@Configuration
@EnableHypermediaSupport(type = {HypermediaType.HAL, HypermediaType.HAL_FORMS})
public class HipermidiaConfiguracao {
    private static final String PROPRIEDADE_HAL_FORMS = "org.springframework.hateoas.mediatype.hal.forms.HalFormsProperty";

    /**
     * Omite {@code readOnly} dos campos dos templates HAL-FORMS.
     *
     * <p>O Spring HATEOAS considera somente leitura toda propriedade sem setter, o que marca todos os componentes de
     * records. As entradas da API são records usados apenas para escrita, então nenhum campo é somente leitura. Pela
     * especificação, a ausência de {@code readOnly} equivale a {@code false}.
     *
     * @return personalização aplicada ao {@code ObjectMapper} do HAL-FORMS
     */
    @Bean
    MediaTypeConfigurationCustomizer<HalFormsConfiguration> camposGravaveis() {
        return configuracao -> !ClassUtils.isPresent(PROPRIEDADE_HAL_FORMS, getClass().getClassLoader()) ? configuracao
            : configuracao.withObjectMapperCustomizer(mapper -> mapper.addMixIn(
                ClassUtils.resolveClassName(PROPRIEDADE_HAL_FORMS, getClass().getClassLoader()), SemSomenteLeitura.class));
    }

    @JsonIgnoreProperties("readOnly")
    private abstract static class SemSomenteLeitura {}
}
