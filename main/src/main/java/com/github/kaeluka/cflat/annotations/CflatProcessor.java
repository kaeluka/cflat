package com.github.kaeluka.cflat.annotations;

import com.github.kaeluka.cflat.ast.TypeSpec;
import com.github.kaeluka.cflat.ast.cflat.backend.IdxClassBackend;
import com.github.kaeluka.cflat.ast.cflat.backend.IdxClassBackendCtx;
import com.github.kaeluka.cflat.ast.cflat.parser.CflatParseError;
import com.github.kaeluka.cflat.ast.cflat.parser.Parser;
import com.google.auto.service.AutoService;
import scala.util.Either;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("com.github.kaeluka.cflat.annotations.Cflat")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class CflatProcessor extends AbstractProcessor {
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (Element _e : roundEnv.getElementsAnnotatedWith(Cflat.class)) {
            assert(_e.getKind() == ElementKind.CLASS);
            TypeElement te = (TypeElement)_e;
            String pe = te.getEnclosingElement().toString();

            System.out.println("te="+te.getSimpleName());
            System.out.println("pe="+pe);
            final Either<CflatParseError, TypeSpec> ethrParsed = Parser.parse(te.getAnnotation(Cflat.class).value());

            if (ethrParsed.isLeft()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Cflat parse error: "+
                                "\n"+ethrParsed.left().get());
                return true;
            }

            final TypeSpec parsed = ethrParsed.right().get();

            System.out.println(("value="+ parsed));


            try {
                final String name = te.getSimpleName() + "Idx";
                final String qualifiedName = te.getQualifiedName() + "Idx";
                System.out.println("creating classfile: " + qualifiedName);
                JavaFileObject jfo = processingEnv
                        .getFiler()
                        .createClassFile(qualifiedName);

                IdxClassBackend b = new IdxClassBackend();
                IdxClassBackendCtx ctx = b
                        .emptyCtx(parsed, pe, name)
                        .withKlassDump(jfo.openOutputStream());
                b.compileProgram(pe, name, parsed, ctx);
            } catch (Throwable e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Cflat error: \n"+e.getMessage());
            }
        }

        return true;
    }
}

