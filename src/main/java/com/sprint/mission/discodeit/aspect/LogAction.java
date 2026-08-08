package com.sprint.mission.discodeit.aspect;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogAction {

    String value();

    String idName() default "";

    int idParamIndex() default -1;
}
