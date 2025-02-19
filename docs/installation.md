# Installation

As Storch is still in an early stage of development, there are no published artifacts available yet. So for the time
being, you have to build it from source:

```bash
git clone https://github.com/sbrunk/storch
cd storch
sbt publishLocal
```

Then, add Storch as a dependency to your project:

@:select(build-tool)

@:choice(sbt)
```scala
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@"
)
```

@:choice(scala-cli)
```scala
//> using scala "3"
//> using repository "sonatype:snapshots"
//> using lib "dev.storch::storch:@VERSION@"
```

@:@

## Adding the native PyTorch libraries

To use Storch, we also need to depend on the native PyTorch libraries (LibTorch),
which are provided by the [JavaCPP](https://github.com/bytedeco/javacpp) project, as part of their autogenerated Java bindings.
There are multiple ways to add the native libraries.

@:callout(info)

Why doesn't Storch just depend on the native PyTorch libraries itself?

Because these are native C++ libraries, which are different for each operating system and architecture.
Furthermore, there are variants for CPU and GPUs which are incompatible with each other.
We don't want to force users on Storch to use one variant over another, so you'll have to add the native dependency yourself.

@:@

### Via PyTorch platform

The easiest and most portable way to depend on the native library is via the PyTorch platform dependency:

@:select(build-tool)

@:choice(sbt)
```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@",
  "org.bytedeco" % "pytorch-platform" % "1.13.1-@JAVACPP_VERSION@"
)
fork := true
```

@:choice(scala-cli)
```scala
//> using repository "sonatype:snapshots"
//> using lib "dev.storch::storch:@VERSION@"
//> using lib "org.bytedeco:pytorch-platform:1.13.1-@JAVACPP_VERSION@"
```

@:@

There is one downside to this approach. Because `pytorch-platform` depends on the native libraries for all supported
platforms, it will download and cache **all** these libraries, no matter on which platform you actually are.

One way to avoid the overhead, is to explicitly depend on the native libraries for **your** platform instead of using
`pytorch-platform`.

### Via classifier

This can be done by providing dependency classifiers specifically for your platform.
Currently supported are `linux-x86_64`, `macosx-x86_64` and `windows-x86_64`.

@:select(build-tool)

@:choice(sbt)
```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@",
  "org.bytedeco" % "pytorch" % "1.13.1-@JAVACPP_VERSION@",
  "org.bytedeco" % "pytorch" % "1.13.1-@JAVACPP_VERSION@" classifier "linux-x86_64"
)
fork := true
```

@:choice(scala-cli)
```scala
//> using repository "sonatype:snapshots"
//> using lib "dev.storch::storch:@VERSION@"
//> using lib "org.bytedeco:openblas:0.3.21-@JAVACPP_VERSION@,classifier=linux-x86_64"
//> using lib "org.bytedeco:pytorch:1.13.1-@JAVACPP_VERSION@,classifier=linux-x86_64"
```

@:@

Now we're only downloading the native libraries for a single platform. The downside though is that the build is not portable anymore.
Fortunately for sbt and Gradle, there's a solution available as a build plugin.

### Automatically detect your platform

The [SBT-JavaCPP](https://github.com/bytedeco/sbt-javacpp) will automatically detect the current platform and set the right classifier.

`project/plugins.sbt`:
```scala
addSbtPlugin("org.bytedeco" % "sbt-javacpp" % "1.17")
```

`build.sbt`:
```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@"
)
javaCppPresetLibs ++= Seq("pytorch" -> "1.13.1")
fork := true

```

If you're using Gradle, you can use the [Gradle JavaCPP](https://github.com/bytedeco/gradle-javacpp) plugin to do the same.

## Enable GPU support

Storch supports GPU accelerated tensor operations for Nvidia GPUs via CUDA. JavaCPP also provides matching CUDA toolkit
distribution including cuDNN, helping you to avoid having to mess with local CUDA installations.


### Via PyTorch platform

@:select(build-tool)

@:choice(sbt)

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@",
  "org.bytedeco" % "pytorch-platform-gpu" % "1.13.1-@JAVACPP_VERSION@",
  "org.bytedeco" % "cuda-platform-redist" % "11.8-8.6-@JAVACPP_VERSION@"
)
fork := true
```

@:choice(scala-cli)
```scala
//> using repository "sonatype:snapshots"
//> using lib "dev.storch::storch:@VERSION@"
//> using lib "org.bytedeco:pytorch-platform-gpu:1.13.1-@JAVACPP_VERSION@"
//> using lib "org.bytedeco:cuda-platform-redist:11.8-8.6-@JAVACPP_VERSION@"
```

@:@

This approach should work on any platform with CUDA support (Linux and Windows) but it causes even more overhead than
the CPU variants as CUDA is quite large. So, to save space and bandwidth you might want to use one of the options below
which work

### Via classifier

This can be done by providing dependency classifiers specifically for your platform.
Currently supported are `linux-x86_64-gpu`, `windows-x86_64-gpu`.

@:select(build-tool)

@:choice(sbt)
```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@",
  "org.bytedeco" % "pytorch" % "1.13.1-@JAVACPP_VERSION@",
  "org.bytedeco" % "pytorch" % "1.13.1-@JAVACPP_VERSION@" classifier "linux-x86_64-gpu"
)
fork := true
```

@:choice(scala-cli)
```scala
//> using repository "sonatype:snapshots"
//> using lib "dev.storch::storch:@VERSION@"
//> using lib "org.bytedeco:pytorch:1.13.1-@JAVACPP_VERSION@,classifier=linux-x86_64-gpu"
```

@:@

### Automatically detect your platform

The [SBT-JavaCPP](https://github.com/bytedeco/sbt-javacpp) also works with the GPU variant by adding `pytorch-gpu`
instead of `pytorch` to `javaCppPresetLibs`.

`project/plugins.sbt`:
```scala
addSbtPlugin("org.bytedeco" % "sbt-javacpp" % "1.17")
```

`build.sbt`:
```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += Seq(
  "dev.storch" % "storch:@VERSION@"
)
javaCppPresetLibs ++= Seq("pytorch-gpu" -> "1.13.1")
fork := true
```

### Running tensor operations on the GPU

```scala mdoc:invisible
torch.manualSeed(0)
```

You can create tensors directly on the GPU:
```scala
import torch.Device.{CPU, CUDA}
val device = if torch.cuda.isAvailable then CUDA else CPU
// device: Device = Device(device = CUDA, index = -1)
torch.rand(Seq(3,3), device=device)
// res1: Tensor[Float32] = dtype=float32, shape=[3, 3], device=CUDA 
// [[0.3990, 0.5167, 0.0249],
//  [0.9401, 0.9459, 0.7967],
//  [0.4150, 0.8203, 0.2290]]

// Use device index if you have multiple GPUs
torch.rand(Seq(3,3), device=torch.Device(torch.DeviceType.CUDA, 0: Byte))
// res2: Tensor[Float32] = dtype=float32, shape=[3, 3], device=CUDA 
// [[0.9722, 0.7910, 0.4690],
//  [0.3300, 0.3345, 0.3783],
//  [0.7640, 0.6405, 0.1103]]
```
Or move them from the CPU:
```scala
val cpuTensor = torch.Tensor(Seq(1,2,3))
// cpuTensor: Tensor[Int32] = dtype=int32, shape=[3], device=CPU 
// [1, 2, 3]
val gpuTensor = cpuTensor.to(device=device)
// gpuTensor: Tensor[Int32] = dtype=int32, shape=[3], device=CUDA 
// [1, 2, 3]
```
