/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.node.module

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ModuleAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.ElideUniversalJsModuleLoader
import com.oracle.truffle.js.runtime.JavaScriptLanguage
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable

// Installs the Node `module` module into the intrinsic bindings.
@Intrinsic internal class NodeModulesModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeModules.obtain() }
  internal fun provide(): ModuleAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.MODULE)) { singleton }
  }
}

/**
 * # Node API: `module`
 */
internal class NodeModules : ReadOnlyProxyObject, ModuleAPI {
  //

  internal companion object {
    private val SINGLETON = NodeModules()
    fun obtain(): NodeModules = SINGLETON
  }


  private val builtins = arrayOf(
    "assert","assert/strict","buffer","child_process","cluster","console","crypto","dgram","diagnostics_channel",
    "dns","dns/promises","domain","events","fs","fs/promises","http","http2","https","inspector","inspector/promises",
    "module","net","os","path","perf_hooks","process","querystring","readline","readline/promises","stream","stream/consumers",
    "stream/promises","stream/web","string_decoder","test","url","util","v8","vm","wasi","worker_threads","zlib"
  )

  override fun getMemberKeys(): Array<String> = arrayOf(
    "builtinModules","createRequire","isBuiltin","register","syncBuiltinESMExports","findSourceMap","SourceMap"
  )
  override fun getMember(key: String?): Any? = when (key) {
    "builtinModules" -> ProxyArray.fromArray(*builtins)
    "isBuiltin" -> ProxyExecutable { args: Array<Value> ->
      val name = args.firstOrNull()?.takeIf { it.isString }?.asString() ?: return@ProxyExecutable false
      builtins.contains(name)
    }
    "createRequire" -> ProxyExecutable { _ ->
      // Return a require() that resolves builtins via ModuleRegistry and JS via Elide's loader when possible.
      ProxyExecutable { argv: Array<Value> ->
        val id = argv.firstOrNull()?.asString() ?: ""
        // Builtins: support both 'node:mod' and 'mod'
        ModuleInfo.find(id.removePrefix("node:"))?.let { return@ProxyExecutable ModuleRegistry.load(it) }
        // Fallback: delegate to global require in the current JS context
        return@ProxyExecutable Context.getCurrent().getBindings("js").getMember("require").execute(id)
      }
    }
    "register" -> ProxyExecutable { _: Array<Value> -> null }
    "syncBuiltinESMExports" -> ProxyExecutable { _: Array<Value> -> null }
    "findSourceMap" -> ProxyExecutable { _: Array<Value> -> null }
    "SourceMap" -> ProxyObject.fromMap(emptyMap<String, Any>())
    else -> null
  }
  override fun getMemberKeys(): Array<String> = arrayOf(
    "builtinModules",
    "isBuiltin",
    "createRequire",
  )

  override fun getMember(key: String?): Any? = when (key) {
    "builtinModules" -> ModuleInfo.allModuleInfos.keys.map { "node:$it" }.toTypedArray()
    "isBuiltin" -> ProxyExecutable { a -> ModuleInfo.find(a[0].asString().removePrefix("node:")) != null }
    "createRequire" -> ProxyExecutable { a -> createRequireFn(a.getOrNull(0)) }
    else -> null
  }

  private fun createRequireFn(from: Value?): ProxyExecutable = ProxyExecutable { argv ->
    val id = argv.getOrNull(0)?.asString() ?: error("require(id) expected")
    ModuleInfo.find(id.removePrefix("node:"))?.let { return@ProxyExecutable ModuleRegistry.load(it) }
    val realm = JavaScriptLanguage.getCurrentJSRealm()
    ElideUniversalJsModuleLoader.resolve(realm, id)?.provide()
      ?: error("Cannot resolve module: $id")
  }
}
