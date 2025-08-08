package it.raptor_service.service.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RaptorRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRaptorRequest {
    String message() default "Invalid Raptor request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}   
