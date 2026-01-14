package com.contentgrid.hateoas.client.hal.forms;

import com.contentgrid.hateoas.client.hal.HalLink;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HalFormsTemplate extends HalFormsTemplateDto {

    @NonNull
    @Getter
    private final HalLink selfLink;

    public HalFormsTemplate(HalLink selfLink, HalFormsTemplateDto template) {
        super(template.getMethod(), template.getTarget(), template.getContentType(), template.getTitle(), template.getProperties());

        this.selfLink = selfLink;
    }


}
