/**
 * Monaco Editor Worker Configuration for Angular 18
 * 
 * Configures self.MonacoEnvironment.getWorker to route TypeScript/JavaScript
 * language services to the bundled ts.worker bundle, enabling full IntelliSense
 * (type-aware completions, signature help) in the browser.
 */

// Define MonacoEnvironment before monaco-editor is imported
self.MonacoEnvironment = {
  getWorker: function (_workerId, label) {
    // Use relative paths that Angular's dev server can resolve
    const workerPaths = {
      'typescript': '/monaco-editor/esm/vs/language/typescript/ts.worker.js',
      'javascript': '/monaco-editor/esm/vs/language/typescript/ts.worker.js',
      'json': '/monaco-editor/esm/vs/language/json/json.worker.js',
      'css': '/monaco-editor/esm/vs/language/css/css.worker.js',
      'scss': '/monaco-editor/esm/vs/language/css/css.worker.js',
      'less': '/monaco-editor/esm/vs/language/css/css.worker.js',
      'html': '/monaco-editor/esm/vs/language/html/html.worker.js',
      'handlebars': '/monaco-editor/esm/vs/language/html/html.worker.js',
      'razor': '/monaco-editor/esm/vs/language/html/html.worker.js'
    };
    
    const workerPath = workerPaths[label] || '/monaco-editor/esm/vs/editor/editor.worker.js';
    
    return new Worker(workerPath, { type: 'module' });
  }
};

