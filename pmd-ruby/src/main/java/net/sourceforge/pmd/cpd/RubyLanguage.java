/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.cpd;

/**
 * Language implementation for Ruby.
 *
 * @author Zev Blut zb@ubit.com
 */
public class RubyLanguage extends AbstractLanguage {

    /**
     * Creates a new Ruby Language instance.
     */
    public RubyLanguage() {
        super("Ruby", "ruby", new AnyTokenizer("#"), ".rb", ".cgi", ".class");
    }
}
