package com.example.cert.service.templates;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class InitializeTemplatesService {

        private final TemplateHelpersService helpersService;
        private TemplateService templateService;

        @Transactional
        public void initializeSystemTemplates() {

                templateService.saveOrUpdateTemplate("Certificado de Participação", "participacao",
                                helpersService.buildParticipacaoSchema());
                templateService.saveOrUpdateTemplate("Certificado de Publicação", "publicacao",
                                helpersService.buildPublicacaoSchema());
                templateService.saveOrUpdateTemplate("Declaração Ad Hoc (Parecerista)", "parecerista",
                                helpersService.buildPareceristSchema());
                templateService.saveOrUpdateTemplate("Declaração de Corpo Editorial", "corpo-editorial",
                                helpersService.buildEditorialSchema());
                templateService.saveOrUpdateTemplate("Declaração de Dossiê Temático", "dossie",
                                helpersService.buildDossieSchema());
                templateService.saveOrUpdateTemplate("Declaração de Aceite de Artigo", "aceite",
                                helpersService.buildAceiteSchema());

        }

}
