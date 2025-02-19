/*
 * Copyright 2022 storch.dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package torch

import org.bytedeco.javacpp.*
import org.bytedeco.pytorch
import org.bytedeco.pytorch.global.torch as torchNative
import org.bytedeco.pytorch.global.torch.{ScalarType, toComplexType}
import org.bytedeco.pytorch.{
  BoolOptional,
  DeviceOptional,
  LayoutOptional,
  LinearImpl,
  LogSoftmaxFuncOptions,
  LongOptional,
  MemoryFormatOptional,
  Module,
  Scalar,
  ScalarTypeOptional,
  TensorArrayRef,
  TensorVector
}

import java.nio.{
  ByteBuffer,
  CharBuffer,
  DoubleBuffer,
  FloatBuffer,
  IntBuffer,
  LongBuffer,
  ShortBuffer
}
import scala.annotation.{targetName, varargs}
import scala.reflect.ClassTag
import ScalarUtils.toScalar
import internal.NativeConverters.*
import Layout.Strided
import Device.CPU
import torch.internal.NativeConverters
import MemoryFormat.Contiguous

import java.nio.file.Path
import java.nio.file.Files
import org.bytedeco.pytorch.GenericDict
import org.bytedeco.pytorch.IValue

import scala.collection.immutable.VectorMap
import scala.collection.immutable.SeqMap
import scala.util.Using

// Creation Ops

// // TODO sparse_coo_tensor
// // TODO as_tensor
// // TODO as_strided
// // TODO frombuffer
// def zeros(size: Int*): Tensor[Float32] = zeros(size.map(_.toLong))

/** Returns a tensor filled with the scalar value `0`, with the shape defined by the variable
  * argument `size`.
  * @param size
  *   a sequence of integers defining the shape of the output tensor.
  * @tparam T
  * @return
  */
def zeros[D <: DType](
    size: Seq[Long],
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[D] =
  Tensor(
    torchNative.torch_zeros(
      size.toArray,
      NativeConverters.tensorOptions(dtype, layout, device, requiresGrad)
    )
  )

def zerosLike[D <: DType, D2 <: DType | Derive](
    input: Tensor[D],
    dtype: D2 = derive,
    layout: Layout | Derive = derive,
    device: Device | Derive = derive,
    requiresGrad: Boolean = false,
    memoryFormat: MemoryFormat = MemoryFormat.Preserve
): Tensor[DTypeOrDeriveFromTensor[D, D2]] =
  val derivedDType = dtype match
    case _: Derive => input.dtype
    case d: DType  => d
  val derivedLayout = layout match
    case _: Derive => input.layout
    case l: Layout => l
  val derivedDevice = device match
    case _: Derive => input.device
    case d: Device => d
  Tensor(
    torchNative.torch_zeros_like(
      input.native,
      NativeConverters.tensorOptions(derivedDType, derivedLayout, derivedDevice, requiresGrad),
      new MemoryFormatOptional(memoryFormat.toNative)
    )
  )

/** Returns a tensor filled with the scalar value `1`, with the shape defined by the variable
  * argument `size`.
  * @param size
  *   a sequence of integers defining the shape of the output tensor.
  * @tparam T
  * @return
  */
def ones[D <: DType](
    size: Seq[Long],
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[D] =
  Tensor(
    torchNative.torch_ones(
      size.map(_.toLong).toArray,
      NativeConverters.tensorOptions(dtype, layout, device, requiresGrad)
    )
  )

def onesLike[D <: DType, D2 <: DType | Derive](
    input: Tensor[D],
    dtype: D2 = derive,
    layout: Layout | Derive = derive,
    device: Device | Derive = derive,
    requiresGrad: Boolean = false,
    memoryFormat: MemoryFormat = MemoryFormat.Preserve
): Tensor[DTypeOrDeriveFromTensor[D, D2]] =
  val derivedDType = dtype match
    case _: Derive => input.dtype
    case d: DType  => d
  val derivedLayout = layout match
    case _: Derive => input.layout
    case l: Layout => l
  val derivedDevice = device match
    case _: Derive => input.device
    case d: Device => d
  Tensor(
    torchNative.torch_ones_like(
      input.native,
      NativeConverters.tensorOptions(derivedDType, derivedLayout, derivedDevice, requiresGrad),
      new MemoryFormatOptional(memoryFormat.toNative)
    )
  )

// format: off
/** Returns a 1-D tensor of size $`\left\lceil \frac{\text{end} - \text{start}}{\text{step}} \right\rceil`$ with values
  * from the interval ``[start, end)`` taken with common difference :attr:`step` beginning from `start`.
  *
  * Note that non-integer `step` is subject to floating point rounding errors when comparing against `end`;
  * to avoid inconsistency, we advise adding a small epsilon to `end` in such cases.
  *
  * $$
  * \text{out}_{{i+1}} = \text{out}_{i} + \text{step}
  * $$
  *
  * @param start
  *   The starting value for the set of points. Default: ``0``.
  * @param end
  *   The ending value for the set of points
  * @param step
  *   The gap between each pair of adjacent points. Default: ``1``.
  */
// format: on
def arange[D <: DType | Derive, Start <: ScalaType, End <: ScalaType, Step <: ScalaType](
    start: Start = 0,
    end: End,
    step: Step = 1,
    dtype: D = derive,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[DTypeOrDeriveArange[D, Start, End, Step]] =
  val derivedDType = dtype match
    case _: Derive => derivedArangeType(start, end, step)
    case t: DType  => t
  Tensor(
    torchNative.torch_arange(
      toScalar(start),
      toScalar(end),
      toScalar(step),
      NativeConverters.tensorOptions(derivedDType, layout, device, requiresGrad)
    )
  )
def linspace[D <: DType](
    start: Double,
    end: Double,
    steps: Long,
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[D] =
  Tensor(
    torchNative.torch_linspace(
      new Scalar(start),
      new Scalar(end),
      steps,
      NativeConverters.tensorOptions(dtype, layout, device, requiresGrad)
    )
  )

def logspace[D <: DType](
    start: Double,
    end: Float,
    steps: Long,
    base: Double = 10.0,
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
) = Tensor(
  torchNative.torch_logspace(
    new Scalar(start),
    new Scalar(end),
    steps,
    base,
    NativeConverters.tensorOptions(dtype, layout, device, requiresGrad)
  )
)
def eye[D <: DType](
    n: Long,
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[D] = Tensor(
  torchNative.torch_eye(n, NativeConverters.tensorOptions(dtype, layout, device, requiresGrad))
)
// def empty(size: Long*): Tensor[D] = Tensor(torchNative.torch_empty(size*))

/** Returns a tensor filled with uninitialized data. */
def empty[D <: DType](
    size: Seq[Long],
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false,
    pinMemory: Boolean = false,
    memoryFormat: MemoryFormat = Contiguous
): Tensor[D] =
  Tensor(
    torchNative.torch_empty(
      size.toArray,
      NativeConverters
        .tensorOptions(dtype, layout, device, requiresGrad)
        .pinned_memory(BoolOptional(pinMemory)),
      new MemoryFormatOptional(memoryFormat.toNative)
    )
  )

/** Returns an uninitialized tensor with the same size as input.
  *
  * `torch.empty_like(input)` is equivalent to `torch.empty(input.size(), dtype=input.dtype,
  * layout=input.layout, device=input.device`).
  */
def emptyLike[D <: DType, D2 <: DType | Derive](
    input: Tensor[D],
    dtype: D2 = derive,
    layout: Layout | Derive = derive,
    device: Device | Derive = derive,
    requiresGrad: Boolean = false,
    memoryFormat: MemoryFormat = MemoryFormat.Preserve
): Tensor[DTypeOrDeriveFromTensor[D, D2]] =
  val derivedDType = dtype match
    case _: Derive => input.dtype
    case d: DType  => d
  val derivedLayout = layout match
    case _: Derive => input.layout
    case l: Layout => l
  val derivedDevice = device match
    case _: Derive => input.device
    case d: Device => d
  Tensor(
    torchNative.torch_empty_like(
      input.native,
      NativeConverters.tensorOptions(derivedDType, derivedLayout, derivedDevice, requiresGrad),
      new MemoryFormatOptional(memoryFormat.toNative)
    )
  )
// // TODO emptyStrided

/** Creates a tensor of size `size` filled with `fillValue`. The tensor's dtype is inferred from
  * `fillValue`.
  *
  * @param size
  *   a sequence of integers defining the shape of the output tensor.
  * @param fillValue
  *   the value to fill the output tensor with.
  * @param dtype
  *   the desired data type of the returned tensor.
  * @param layout
  *   the desired layout of the returned Tensor.
  * @param device
  *   the desired device of the returned tensor.
  * @param requiresGrad
  *   If autograd should record operations on the returned tensor.
  * @tparam T
  *   the data type of the returned tensor, or `Default` if the type should be derived from
  *   `fillValue`.
  * @tparam U
  *   the data type of `fillValue`.
  * @return
  *   the newly created tensor.
  */
def full[D <: DType | Derive, U <: ScalaType](
    size: Seq[Long],
    fillValue: U,
    dtype: D = derive,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[DTypeOrDeriveFromScalar[D, U]] =
  val derivedDType = dtype match
    case _: Derive => scalaToDType(fillValue)
    case t: DType  => t
  Tensor(
    torchNative.torch_full(
      size.toArray,
      toScalar(fillValue),
      NativeConverters.tensorOptions(derivedDType, layout, device, requiresGrad)
    )
  )
// TODO fullLike
// TODO quantize_per_tensor
// TODO quantize_per_channel
// TODO dequantize
// TODO complex
// TODO polar
// TODO heavside

def pickleLoad(data: Array[Byte]): SeqMap[String, Tensor[DType]] =
  val dict: GenericDict = torchNative.pickle_load(data).toGenericDict()
  // We need to extract the members in one go or we risk too early deallocation of native objects here
  val buffer = new Array[(IValue, IValue)](dict.size().toInt)
  val nativeIt = dict.begin()
  for (i <- 0 until buffer.size)
    buffer(i) = (nativeIt.access().key(), nativeIt.access().value())
    nativeIt.increment()
  VectorMap.from(buffer.map { (key, value) =>
    // TODO better error handling
    (key.toStringRef().getString(), Tensor[DType](value.toTensor().clone()))
  })

def pickleLoad(path: Path): Map[String, Tensor[DType]] =
  val data: Array[Byte] = Files.readAllBytes(path)
  pickleLoad(data)

def pickle_save(tensors: SeqMap[String, Tensor[DType]]) =
  tensors.map { (k, v) =>
    (IValue(k), IValue(v.native))
  }

/** Returns a tensor filled with random numbers from a uniform distribution on the interval `[0,1)`
  *
  * The shape of the tensor is defined by the variable argument `size`.
  *
  * @param size
  *   a sequence of integers defining the shape of the output tensor.
  * @param dtype
  *   the desired data type of returned tensor.
  * @param layout
  *   the desired layout of returned Tensor.
  * @param device
  *   the desired device of returned tensor.
  * @param requiresGrad
  *   If autograd should record operations on the returned tensor.
  * @tparam T
  *   the dtype of the created tensor.
  */
def rand[D <: FloatNN | ComplexNN](
    size: Seq[Long],
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[D] =
  Tensor(
    torchNative.torch_rand(
      size.toArray,
      NativeConverters.tensorOptions(dtype, layout, device, requiresGrad)
    )
  )

def randn[D <: FloatNN](
    size: Seq[Long],
    dtype: D = float32,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false
): Tensor[D] =
  Tensor(
    torchNative.torch_rand(
      size.toArray,
      NativeConverters.tensorOptions(dtype, layout, device, requiresGrad)
    )
  )

/** Returns a random permutation of integers from 0 to n - 1.
  *
  * TODO support custom generator
  */
def randperm[D <: DType](
    n: Long,
    dtype: D = int64,
    layout: Layout = Strided,
    device: Device = CPU,
    requiresGrad: Boolean = false,
    pinMemory: Boolean = false
): Tensor[D] =
  Tensor(
    torchNative.torch_randperm(
      n,
      NativeConverters.tensorOptions(dtype, layout, device, requiresGrad, pinMemory)
    )
  )

// End Creation Ops

// Indexing, Slicing, Joining, Mutating Ops

def cat[D <: DType](tensors: Seq[Tensor[D]], dim: Int = 0): Tensor[D] = Tensor(
  torchNative.cat(new TensorArrayRef(new TensorVector(tensors.map(_.native)*)), dim.toLong)
)

/** Concatenates a sequence of tensors along a new dimension.
  *
  * All tensors need to be of the same size.
  */
def stack[D <: DType](tensors: Seq[Tensor[D]], dim: Int = 0): Tensor[D] = Tensor(
  torchNative.stack(new TensorArrayRef(new TensorVector(tensors.map(_.native)*)), dim)
)

// End Indexing, Slicing, Joining, Mutating Ops

def manualSeed(seed: Long) = torchNative.manual_seed(seed)

/** Disable gradient calculation for [[op]].
  *
  * Disabling gradient calculation is useful for inference, when you are sure that you will not call
  * `Tensor.backward()`. It will reduce memory consumption for computations that would otherwise
  * have `requiresGrad=true`.
  *
  * In this mode, the result of every computation will have `requiresGrad=false`, even when the
  * inputs have `requiresGrad=true`.
  *
  * This context manager is thread local; it will not affect computation in other threads.
  *
  * @param op
  */
def noGrad[A](op: => A): A = {
  import org.bytedeco.pytorch.NoGradGuard
  Using.resource(NoGradGuard()) { _ =>
    op
  }
}

def setNumThreads(threads: Int): Unit = torchNative.set_num_threads(threads)
