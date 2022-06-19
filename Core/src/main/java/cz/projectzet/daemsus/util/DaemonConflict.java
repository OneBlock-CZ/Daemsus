package cz.projectzet.daemsus.util;

import cz.projectzet.daemsus.AbstractDaemon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DaemonConflict {

    Class<? extends AbstractDaemon<?>>[] value();

}
