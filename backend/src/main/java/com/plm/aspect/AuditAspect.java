package com.plm.aspect;

import com.plm.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Pointcut("execution(* com.plm.service.ItemService.createItem(..)) ||" +
              "execution(* com.plm.service.ItemService.updateItem(..)) ||" +
              "execution(* com.plm.service.ItemService.deleteItem(..)) ||" +
              "execution(* com.plm.service.ItemService.transitionLifecycle(..)) ||" +
              "execution(* com.plm.service.RevisionService.nextRevision(..)) ||" +
              "execution(* com.plm.service.RevisionService.updateStatus(..)) ||" +
              "execution(* com.plm.service.DocumentService.uploadDocument(..)) ||" +
              "execution(* com.plm.service.DocumentService.deleteDocument(..)) ||" +
              "execution(* com.plm.service.ChangeRequestService.create(..)) ||" +
              "execution(* com.plm.service.ChangeRequestService.update(..)) ||" +
              "execution(* com.plm.service.ChangeRequestService.delete(..)) ||" +
              "execution(* com.plm.service.ChangeRequestService.submit(..)) ||" +
              "execution(* com.plm.service.ChangeRequestService.approve(..)) ||" +
              "execution(* com.plm.service.ChangeRequestService.reject(..))")
    public void auditedMethods() {}

    @AfterReturning(pointcut = "auditedMethods()", returning = "result")
    public void afterReturning(JoinPoint jp, Object result) {
        try {
            String methodName = jp.getSignature().getName();
            String entityType = resolveEntityType(jp.getSignature().getDeclaringTypeName());
            Long entityId = resolveId(result, jp.getArgs());
            String details = buildDetails(methodName, jp.getArgs());
            auditLogService.log(methodName, entityType, entityId, details);
        } catch (Exception e) {
            log.warn("Audit logging failed: {}", e.getMessage());
        }
    }

    private String resolveEntityType(String className) {
        if (className.contains("Item")) return "Item";
        if (className.contains("Revision")) return "Revision";
        if (className.contains("Document")) return "Document";
        if (className.contains("ChangeRequest")) return "ChangeRequest";
        return "Unknown";
    }

    private Long resolveId(Object result, Object[] args) {
        if (result != null) {
            try {
                Method getId = result.getClass().getMethod("getId");
                Object id = getId.invoke(result);
                if (id instanceof Long l) return l;
            } catch (Exception ignored) {}
        }
        // fall back to first Long arg
        for (Object arg : args) {
            if (arg instanceof Long l) return l;
        }
        return null;
    }

    private String buildDetails(String method, Object[] args) {
        if (args.length > 0 && args[0] != null) {
            String first = args[0].toString();
            return method + ": " + (first.length() > 100 ? first.substring(0, 100) + "…" : first);
        }
        return method;
    }
}
