const EFFECT_CALL_MESSAGE = 'effect() must be called only in a constructor or field initializer (Angular NG0203)';

export default {
  meta: {
    type: 'problem',
    docs: {
      description: 'Disallows effect() calls outside constructors and field initializers to prevent Angular NG0203 runtime errors.',
    },
    messages: {
      noEffectOutsideScope: EFFECT_CALL_MESSAGE,
    },
  },

  create(context) {
    function isEffectCall(node) {
      return (
        node.type === 'CallExpression' &&
        node.callee.type === 'Identifier' &&
        node.callee.name === 'effect'
      );
    }

    function isConstructorScope(ancestorNode) {
      if (!ancestorNode || ancestorNode.type !== 'MethodDefinition') return false;
      return ancestorNode.kind === 'constructor';
    }

    function isFieldInitializer(ancestorNode) {
      if (!ancestorNode) return false;
      return ancestorNode.type === 'PropertyDefinition' && ancestorNode.value != null;
    }

    function findEnclosingScope(node, ancestors) {
      for (let i = ancestors.length - 1; i >= 0; i--) {
        const anc = ancestors[i];
        if (isConstructorScope(anc)) return 'constructor';
        if (isFieldInitializer(anc)) return 'field';
      }
      return null;
    }

    function checkCallExpression(node) {
      if (!isEffectCall(node)) return;

      const ancestors = context.sourceCode ? context.sourceCode.getAncestors(node) : [];
      const scope = findEnclosingScope(node, ancestors);

      if (scope !== 'constructor' && scope !== 'field') {
        context.report({ node, messageId: 'noEffectOutsideScope' });
      }
    }

    return {
      CallExpression: checkCallExpression,
    };
  },
};
