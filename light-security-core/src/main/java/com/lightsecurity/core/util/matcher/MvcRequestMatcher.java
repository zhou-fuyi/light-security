package com.lightsecurity.core.util.matcher;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

public class MvcRequestMatcher implements RequestMatcher, RequestVariablesExtractor {

    private final DefaultMatcher defaultMatcher = new DefaultMatcher();

    private final HandlerMappingIntrospector introspector;
    private final String pattern;
    private HttpMethod method;
    private String servletPath;

    public MvcRequestMatcher(HandlerMappingIntrospector introspector, String pattern) {
        this.introspector = introspector;
        this.pattern = pattern;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        if (this.method != null && !this.method.name().equals(request.getMethod())) {
            return false;
        }
        if (this.servletPath != null
                && !this.servletPath.equals(request.getServletPath())) {
            return false;
        }
        MatchableHandlerMapping mapping = getMapping(request);
        if (mapping == null) {
            return this.defaultMatcher.matches(request);
        }
        RequestMatchResult matchResult = mapping.match(request, this.pattern);
        return matchResult != null;
    }

    private MatchableHandlerMapping getMapping(HttpServletRequest request) {
        try {
            return this.introspector.getMatchableHandlerMapping(request);
        }
        catch (Throwable t) {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.security.web.util.matcher.RequestVariablesExtractor#
     * extractUriTemplateVariables(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public Map<String, String> extractUriTemplateVariables(HttpServletRequest request) {
        MatchableHandlerMapping mapping = getMapping(request);
        if (mapping == null) {
            return this.defaultMatcher.extractUriTemplateVariables(request);
        }
        RequestMatchResult result = mapping.match(request, this.pattern);
        return result == null ? Collections.<String, String>emptyMap()
                : result.extractUriTemplateVariables();
    }

    /**
     * @param method the method to set
     */
    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    /**
     * The servlet path to match on. The default is undefined which means any servlet
     * path.
     *
     * @param servletPath the servletPath to set
     */
    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    protected final String getServletPath() {
        return this.servletPath;
    }

    private class DefaultMatcher implements RequestMatcher, RequestVariablesExtractor {

        private final UrlPathHelper pathHelper = new UrlPathHelper();

        private final PathMatcher pathMatcher = new AntPathMatcher();

        @Override
        public boolean matches(HttpServletRequest request) {
            String lookupPath = this.pathHelper.getLookupPathForRequest(request);
            return matches(lookupPath);
        }

        private boolean matches(String lookupPath) {
            return this.pathMatcher.match(MvcRequestMatcher.this.pattern, lookupPath);
        }

        @Override
        public Map<String, String> extractUriTemplateVariables(
                HttpServletRequest request) {
            String lookupPath = this.pathHelper.getLookupPathForRequest(request);
            if (matches(lookupPath)) {
                return this.pathMatcher.extractUriTemplateVariables(
                        MvcRequestMatcher.this.pattern, lookupPath);
            }
            return Collections.emptyMap();
        }
    }
}
