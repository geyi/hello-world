## 1
### 1.1
#### 1.1.1

# kubernetes
* 一个数据中心操作系统？
* 虚拟机是以虚拟一台机器为边界，其问题域比较闭合。但容器计算就是要打破机器的边界，让计算力在数据中心内自由调度。这问题涉及面非常广，除了常规的计算力调度、负载均衡、部署升级、服务发现、日志与监控，这些东西都要以全新的方式来解决。
* Docker、Kubernets都是基于GoLang来实现的。
* Docker的轻量级、Kubernets的灵活性和开放性，能让我们以API调用的方式来交付计算力（包括CPU、内存、网络、磁盘等），能让业务应用摆脱传统资源交付方式和既往运维手段的束缚，更快捷地得到部署、升级和管理，业务迭代速度大大加快，资源利用率大幅提高。（提高部署和运维效率）（熟练地掌握和使用Kubernetes，将成为每个前后端工程师的必备技能，Kubernetes将成为发布前后端服务的标准途径）
* Kubernetes要做的：调度、配置、监控和故障处理
* Kubernetes抽象了数据中心的硬件基础设施，使得对外暴露的只是一个巨大的资源池。它让我们在部署和运行组件是，不用关注底层服务器。使用Kubernetes部署多组件应用时，它会为每个组件都选择一个合适的服务器，部署之后它能够保证每个组件可以轻易地发现其它组件，并彼此之间实现通信。

## 1 Kubernetes介绍
### 1.1 Kubernetes系统的需求
#### 1.1.1 从单体应用到微服务
* 应用程序开发部署的变化
	1. 大型单体应用 --> 微服务
	2. 基础架构的变化

**大型单体应用**
* 开发者会把应用程序打包后交付给运维团队，运维人员再处理部署、监控事宜，并且在硬件发生故障时手动迁移应用。

**微服务**
* 大型单体用用被逐渐分解成小的、可独立运行的组件，我们称之为微服务。微服务彼此之间解耦，所以它们可以被独立开发、部署、升级、伸缩。这使得我们可以对每一个微服务实现快速迭代。

1. 但是，随着部署组件的增多和数据中心的增长，配置、管理并保持系统的正常运行变得越来越困难（微服务的部署、扩容）。
2. 如果我们想要获得足够高的资源利用率并降低硬件成本，把组件部署在什么地方变得越来越难以决策。
3. 微服务还带来了其它问题，比如因为跨了多个进程和机器，使得调式代码和定位异常调用变的困难。（幸运的是，这些问题现在已经被诸如Zipkin这样的分布式定位系统解决）

#### 1.1.2 一个一致的环境
* 为了减少仅会在生产环境才暴露的问题，最理想的做法是让应用在开发和生产阶段可以运行在完全一样的环境下，它们有一样的操作系统、库、系统配置、网络环境和其它所有的条件（环境需求的差异）。

#### 1.1.3 DevOps和无运维
> 软件开发端(Dev)到系统维护端(Ops)
* DevOps带来的优点：让开发者对用户的需求和问题，以及运维团队维护应用所面临的困难，有一个更好的了解。
* DevOps存在问题：开发人员通常不了解或者不想了解数据中心底层设备和架构。

* 运维人员不想处理所有应用组件之间暗藏的依赖关系，不想考虑底层操作系统或基础设施的改变会怎样影响应用程序，但是他们却不得不关注这些事情。

* 理想的情况是
	1. 开发人员部署程序本身，不需要知道硬件基础设施的任何情况，也不需要和运维团队交涉，这被叫作NoOps。
	2. 运维人员聚焦于保持底层基础设施运转正常的同时，不需要关注实际运行在平台上的应用程序。
	
* Kubernetes能让我们实现所有这些想法。通过对实际硬件做抽象，然后将自身暴露成一个平台，用于部署和运行应用程序。

### 1.2容器技术
* 一个问题：当一个应用程序仅由较少数量的大组件构成时，完全可以接受给每个组件分配专用的虚拟机，以及通过给每个组件提供自己的操作系统实例来隔离它们的环境。但是当这些组件开始被拆分成许多小的组件且数量开始增长时，如果你不想浪费硬件资源，又想持续压低硬件成本，那就不能给每个组件配置一个虚拟机了。另一方面，虚拟机数量的增加，将会带来运维人员的工作负担，导致了人力资源的浪费。
1. 轻量：在一台机器上运行的多个Docker容器可以共享这台机器的操作系统内核；它们能够迅速启动，只需占用很少的计算和内存资源。镜像是通过文件系统层进行构造的，并共享一些公共文件。这样就能尽量降低磁盘用量，并能更快地下载镜像。
2. 标准：Docker 容器基于开放式标准，能够在所有主流 Linux 版本、Microsoft Windows 以及包括 VM、裸机服务器和云在内的任何基础设施上运行。
3. 安全：Docker 赋予应用的隔离性不仅限于彼此隔离，还独立于底层的基础设施。Docker 默认提供最强的隔离，因此应用出现问题，也只是单个容器的问题，而不会波及到整台机器。

**比较虚拟机和容器**
* 容器和虚拟机具有相似的资源隔离和分配优势，但功能有所不同，因为容器虚拟化的是操作系统，而不是硬件，因此容器更容易移植，效率也更高。
* 容器：容器是一个应用层抽象，用于将代码和依赖资源打包在一起。多个容器可以在同一台机器上运行，共享操作系统内核，但各自作为独立的进程在用户空间中运行。与虚拟机相比，容器占用的空间较少（容器镜像大小通常只有几十兆），瞬间就能完成启动。
* 虚拟机：虚拟机 (VM) 是一个物理硬件层抽象，用于将一台服务器变成多台服务器。管理程序允许多个 VM 在一台机器上运行。每个 VM 都包含一整套操作系统、一个或多个应用、必要的二进制文件和库资源，因此占用大量空间。而且 VM 启动也十分缓慢。

**容器实现隔离的机制**
1. Linux命名空间，它使每个进程只看到它自己的系统视图（文件、进程、网络接口、主机名等）
2. Linux控制组，它限制了进程能使用的资源量（CPU、内存、网络带宽等）


## 2 开始使用Kubernetes和Docker
### 2.1 docker命令
* 运行一个容器：docker run <image>
	1. 首先Docker会检查镜像是否存在于本机。
	2. 如果没有，Docker会从镜像中心拉取镜像。
	3. 镜像下载到本机之后，Docker基于这个镜像创建一个容器并在容器中运行命令。
* 构建容器镜像：docker build -t <image name> .
* 查看镜像：docker images
* 运行镜像：docker run --name <container name> -p 8080:8080 -d <image name>
* 查看运行中的容器：docker ps
* 查看容器信息：docker inspect <container name>
* 在已有的容器内部运行shell：docker exec -it <container name> bash
	1. -i，确保标准输入流保持开放
	2. -t，分配一个伪终端
* 停止容器：docker stop <container name>
* 删除容器：docker rm <container name>
* 创建tag：docker tag <image id> geyi/hello-docker:v1.1
* push tag：docker push geyi/hello-docker:v1.1

### 2.2 构建一个Kubernetes
* Install with Homebrew on macOS
	1. 安装命令：`brew install kubernetes-cli`
	2. 检查版本：`kubectl version`
* Enabling shell autocompletion (Zsh)
	1. vim ~/.zshrc
	2. source <(kubectl completion zsh)
* 运行k8s：minikube start


## 3 pods
### 3.2 使用yaml文件创建pod
* 通过yaml文件创建pod：kubectl create -f hello-docker.yaml
* 查看应用程序的日志：kubectl logs hello-docker-manual
* 将本机的8888端口映射到pod的8080端口：kubectl port-forward hello-docker-manual 8888:8080（kubectl port-forward <pod name> <local port>:<pod port>）
* 访问服务：
~/Docker » curl localhost:8888/docker/hello
Hello Docker, this is hello-docker-manual, ip is 172.17.0.7%

### 3.3 使用标签组织pod
* 显示pod的标签：kubectl get po --show-labels
* 使用-L来显示指定标签：kubectl get po -L creation_method,env
* 给pod增加标签：kubectl label po hello-docker-manual creation_method=manual (kubectl label po <pod name> <label key>=<label value>)
* 使用--overwrite来修改pod标签的值：kubectl label pod hello-docker-manual-v2 env=test --overwrite
* 删除pod的标签：kubectl label pods hello-docker-manual evn-

### 3.4 通过标签选择器列出pod子集
* 使用标签选择器列出pod：
	1. kubectl get po -l creation_method=manual
	2. kubectl get po -l env
	3. kubectl get po -l '!env'
	4. kubectl get po -l creation_method!=manual
	5. kubectl get po -l "env in (test)"
	6. kubectl get po -l "env notin (test)"
* 在标签选择器中使用多个条件：kubectl get po -l creation_method=manual,env=test

### 3.5 使用标签和选择器来约束pod调度
* 使用标签分类工作节点：kubectl label nodes minikube gpu=true
* 使用标签选择器列出node：kubectl get nodes -l gpu=true
* 将pod调度到特定节点
spec:
  nodeSelector:
    gpu: "true"

### 3.6 注解

### 3.7 使用命名空间对资源进行分组
* 列出指定命名空间下的pod：kubectl get po -n kube-system
* 创建命名空间：
apiVersion: v1
kind: Namespace
metadata:
  name: custom-namespace

* 创建命名空间：kubectl create namespace custom-namespace 
* 在指定的命名空间下创建pod：kubectl create -f hello-docker.yaml -n custom-namespace

### 3.8 删除pod
* 通过pod的名称删除pod：kubectl delete po hello-docker-manual
* 通过标签选择器删除pod：kubectl delete po -l creation_method=manual
* 删除整个命名空间（命名空间下的所有pod会被删除）：kubectl delete ns custom-namespace
* 删除命名空间下的所有pod：kubectl delete po -all
* 删除命名空间下的所有资源：kubectl delete all -all


## 4 副本机制和其他控制器：部署托管的 pod
### 4.1 保持 pod 健康
































