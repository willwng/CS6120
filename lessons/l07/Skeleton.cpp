#include "llvm/Pass.h"
#include "llvm/Passes/PassBuilder.h"
#include "llvm/Passes/PassPlugin.h"
#include "llvm/Support/raw_ostream.h"

using namespace llvm;

namespace {

struct SkeletonPass : public PassInfoMixin<SkeletonPass> {
    PreservedAnalyses run(Module &M, ModuleAnalysisManager &AM) {
        for (auto &F : M) {
			for (auto &B : F) {
				for (auto &I : B) {
                    if(auto *branch = dyn_cast<BranchInst>(&I)) {
					    if (branch->getNumSuccessors() > 1) {
					        Value *condition = branch->getCondition();
                            errs() << "Before: "<< I << "\n";
                            IRBuilder<> builder(branch);
					        Value *negated = builder.CreateICmpEQ(condition, Constant::getNullValue(condition->getType()));
                            branch->setCondition(negated);
                            branch->swapSuccessors();
                            errs() << "After: "<< I << "\n";
					    }
					}
				}
			}
        }
        return PreservedAnalyses::none();
    };
};
}


extern "C" LLVM_ATTRIBUTE_WEAK ::llvm::PassPluginLibraryInfo
llvmGetPassPluginInfo() {
    return {
        .APIVersion = LLVM_PLUGIN_API_VERSION,
        .PluginName = "Skeleton pass",
        .PluginVersion = "v0.1",
        .RegisterPassBuilderCallbacks = [](PassBuilder &PB) {
            PB.registerPipelineStartEPCallback(
                [](ModulePassManager &MPM, OptimizationLevel Level) {
                    MPM.addPass(SkeletonPass());
                });
        }
    };
}
