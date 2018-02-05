package com.github.kaeluka.cflat.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cflat {
    public String value();
}

