package org.igot.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ComponentScan(basePackages = { "org.igot.common" })
@Import(CommonConfig.class)
public class CommonAutoConfiguration {
}
