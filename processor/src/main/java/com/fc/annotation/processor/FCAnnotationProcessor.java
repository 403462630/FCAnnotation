package com.fc.annotation.processor;

import com.fc.annotation.Debounce;
import com.fc.annotation.Delay;
import com.fc.annotation.Throttle;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.LinkedHashSet;
import java.util.Set;

public class FCAnnotationProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Debounce.class.getCanonicalName());
        types.add(Throttle.class.getCanonicalName());
        types.add(Delay.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                if (element instanceof ExecutableElement) {
                    boolean privateFlag = false;
                    boolean finalFlag = false;
                    boolean staticFlag = false;
                    for (Modifier modifier : element.getModifiers()) {
                        if (modifier == Modifier.PRIVATE) {
                            privateFlag = true;
                        } else if (modifier == Modifier.FINAL) {
                            finalFlag = true;
                        } else if (modifier == Modifier.STATIC) {
                            staticFlag = true;
                        }
                    }
                    if (privateFlag) {
                        return true;
                    }
                    if (!finalFlag) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getSimpleName() + " 注解必须使用 private 或 final 修饰符", element);
                        return true;
                    }

                    if (staticFlag) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@" + annotation.getSimpleName() + " 注解不支持 static 方法", element);
                        return true;
                    }
                }
            }
        }
        return true;
    }

    private void println(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message);
    }
}
