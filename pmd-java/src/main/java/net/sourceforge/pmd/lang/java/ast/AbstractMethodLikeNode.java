/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.ast;

import net.sourceforge.pmd.lang.java.qname.JavaOperationQualifiedName;

abstract class AbstractMethodLikeNode extends AbstractJavaAccessNode implements MethodLikeNode {

    private JavaOperationQualifiedName qualifiedName;


    AbstractMethodLikeNode(int i) {
        super(i);
    }


    void setQualifiedName(JavaOperationQualifiedName qualifiedName) {
        this.qualifiedName = qualifiedName;
    }


    @Override
    @Deprecated
    public JavaOperationQualifiedName getQualifiedName() {
        return qualifiedName;
    }

}
