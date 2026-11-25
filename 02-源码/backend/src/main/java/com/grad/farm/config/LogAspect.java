package com.grad.farm.config;

import com.grad.farm.common.UserContext;
import com.grad.farm.entity.SysLog;
import com.grad.farm.mapper.SysLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志切面：只记录写操作（POST/PUT/DELETE），异步失败不影响主流程。
 * 日志中的参数不落库敏感字段，避免密码外泄。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysLogMapper logMapper;

    @Around("execution(* com.grad.farm.controller..*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().getName();
        Object result;
        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            write(pjp, method, System.currentTimeMillis() - start, "FAIL");
            throw t;
        }
        write(pjp, method, System.currentTimeMillis() - start, "OK");
        return result;
    }

    private void write(ProceedingJoinPoint pjp, String method, long cost, String status) {
        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()) == null
                    ? null : ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            if (req == null || "GET".equalsIgnoreCase(req.getMethod())) {
                return;
            }
            SysLog l = new SysLog();
            l.setUserId(UserContext.userId());
            l.setUsername(UserContext.get() == null ? "anonymous" : UserContext.get().username());
            l.setModule(pjp.getTarget().getClass().getSimpleName().replace("Controller", ""));
            l.setAction(method);
            l.setMethod(req.getMethod() + " " + req.getRequestURI());
            l.setParams("");
            l.setIp(req.getRemoteAddr());
            l.setCostMs(cost);
            l.setResult(status);
            logMapper.insert(l);
        } catch (Exception e) {
            log.debug("操作日志写入失败：{}", e.getMessage());
        }
    }
}
