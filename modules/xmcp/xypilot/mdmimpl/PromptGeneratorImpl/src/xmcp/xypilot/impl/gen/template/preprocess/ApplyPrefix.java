/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package xmcp.xypilot.impl.gen.template.preprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.TemplateDirectiveModel;
import xmcp.xypilot.impl.gen.template.ApplyDirective;
import xmcp.xypilot.impl.gen.util.StringUtils;

/**
 * Applies prefixes across line breaks for lines generated by variables and macro calls in a template.
 * Required directives: {@link ApplyDirective}
 * Dependencies: {@link RemoveComments}
 */
public class ApplyPrefix implements TemplatePreprocessor {

    private static final String APPLY_DIRECTIVE = "<@apply prefix=\"%s\" suffix=\"%s\">%s</@apply>";

    private static final String DIRECTIVE_REGEX = "<(#|@).+?/?>";
    private static final String USER_DIRECTIVE_CALL_REGEX = "<@.+?/>";
    private static final String VARIABLE_REGEX = "\\$\\{.+?\\}";
    private static final String VARIABLE_OR_DIRECTIVE_REGEX = VARIABLE_REGEX + "|" + DIRECTIVE_REGEX;
    private static final String APPLY_PREFIX_TO_REGEX = "(.*)(" + USER_DIRECTIVE_CALL_REGEX + "|" + VARIABLE_REGEX + ")(.*)";

    private static final Pattern USER_DIRECTIVE_CALL = Pattern.compile(USER_DIRECTIVE_CALL_REGEX);
    private static final Pattern VARIABLE_OR_DIRECTIVE = Pattern.compile(VARIABLE_OR_DIRECTIVE_REGEX);
    private static final Pattern APPLY_PREFIX_TO = Pattern.compile(APPLY_PREFIX_TO_REGEX);


    @Override
    public Reader process(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();

        // read line by line
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(applyPrefix(line));
            sb.append("\n");
        }

        return new StringReader(sb.toString());
    }


    @Override
    public Map<String, Class<? extends TemplateDirectiveModel>> requiredDirectives() {
        return Map.of("apply", ApplyDirective.class);
    }


    @Override
    public List<Class<? extends TemplatePreprocessor>> dependencies() {
        return List.of(RemoveComments.class);
    }


    private String applyPrefix(String code) {
        Matcher applyPrefixTo = APPLY_PREFIX_TO.matcher(code);
        Matcher directiveCall = USER_DIRECTIVE_CALL.matcher(code);
        Matcher variableOrDirective = VARIABLE_OR_DIRECTIVE.matcher(code);

        if (applyPrefixTo.matches() && variableOrDirective.results().count() == 1) {
            // only apply text prefix, no directives or variables

            String prefix = applyPrefixTo.group(1);
            String suffix = applyPrefixTo.group(3);

            // preserve prefix of directive/macro calls and variables across line breaks by wrapping the macro call in a <@apply prefix> directive
            // the suffix is appended after the prefixes are applied
            // in case of a directive/macro call, a newline is appended to the suffix
            // this is the default behavior for variables but somehow not for directive/macro calls
            // the apply directive discards any output, if the generated body (including the suffix) is blank
            if (!prefix.isEmpty()) {
                code = String.format(
                    APPLY_DIRECTIVE,
                    StringUtils.escapeQuotes(prefix),
                    StringUtils.escapeQuotes(directiveCall.find() ? suffix + "\n" : suffix),
                    code.substring(prefix.length(), code.length() - suffix.length())
                );
            }
        }
        return code;
    }

}
