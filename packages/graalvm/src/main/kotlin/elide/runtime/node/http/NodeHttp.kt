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
package elide.runtime.node.http

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.HTTPAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider

// Installs the Node `http` module into the intrinsic bindings.
@Intrinsic internal class NodeHttpModule(
  private val exec: GuestExecutorProvider = GuestExecutorProvider { GuestExecution.direct() }
) : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp.create(exec) }
  internal fun provide(): HTTPAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP)) { provide() }
  }
}

/** Minimal placeholder object type */
private class ReadOnlyTypeObject(private val name: String) : ReadOnlyProxyObject {
  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
  override fun toString(): String = "[object $name]"
}

/**
 * # Node API: `http`
 * Minimal shape to satisfy conformance tests; behavior filled elsewhere.
 */
internal class NodeHttp private constructor () : ReadOnlyProxyObject, HTTPAPI {
  //

  internal companion object {
    @JvmStatic fun create(@Suppress("UNUSED_PARAMETER") exec: GuestExecutorProvider? = null): NodeHttp = NodeHttp()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
