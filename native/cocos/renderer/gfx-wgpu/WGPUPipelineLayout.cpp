/****************************************************************************
 Copyright (c) 2020-2021 Xiamen Yaji Software Co., Ltd.

 http://www.cocos.com

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated engine source code (the "Software"), a limited,
 worldwide, royalty-free, non-assignable, revocable and non-exclusive license
 to use Cocos Creator solely to develop games on your target platforms. You shall
 not use Cocos Creator software for developing other software or tools that's
 used for developing games. You are not granted to publish, distribute,
 sublicense, and/or sell copies of Cocos Creator.

 The software or tools in this License Agreement are licensed, not sold.
 Xiamen Yaji Software Co., Ltd. reserves all rights not expressly granted to you.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
****************************************************************************/

#include "WGPUPipelineLayout.h"
#include <emscripten/html5_webgpu.h>
#include "WGPUDescriptorSetLayout.h"
#include "WGPUDevice.h"
#include "WGPUObject.h"
#include "base/std/container/vector.h"

namespace cc {
namespace gfx {

ccstd::map<ccstd::hash_t, void *> CCWGPUPipelineLayout::layoutMap;

using namespace emscripten;

CCWGPUPipelineLayout::CCWGPUPipelineLayout() : PipelineLayout() {
}

CCWGPUPipelineLayout::~CCWGPUPipelineLayout() {
    doDestroy();
}

void CCWGPUPipelineLayout::doInit(const PipelineLayoutInfo &info) {
    _gpuPipelineLayoutObj = ccnew CCWGPUPipelineLayoutObject;
}

void CCWGPUPipelineLayout::prepare(const ccstd::set<uint8_t> &setInUse) {
    ccstd::hash_t hash = _setLayouts.size() * 2 + 1;
    ccstd::hash_combine(hash, _setLayouts.size());
    ccstd::vector<WGPUBindGroupLayout> layouts;
    for (size_t i = 0; i < _setLayouts.size(); i++) {
        auto *descriptorSetLayout = static_cast<CCWGPUDescriptorSetLayout *>(_setLayouts[i]);
        if (setInUse.find(i) == setInUse.end()) {
            // give it default bindgrouplayout if not in use
            layouts.push_back(static_cast<WGPUBindGroupLayout>(CCWGPUDescriptorSetLayout::defaultBindGroupLayout()));
            ccstd::hash_combine(hash, i);
            ccstd::hash_combine(hash, 9527);
        } else {
            if (!descriptorSetLayout->gpuLayoutEntryObject()->bindGroupLayout) {
                printf("bgl in ppl is null\n");
                while (1) {
                }
            }
            layouts.push_back(descriptorSetLayout->gpuLayoutEntryObject()->bindGroupLayout);
            ccstd::hash_combine(hash, i);
            ccstd::hash_combine(hash, descriptorSetLayout->getHash());
        }
    }

    _hash = hash;

    WGPUPipelineLayoutDescriptor descriptor = {
        .nextInChain = nullptr,
        .label = nullptr,
        .bindGroupLayoutCount = layouts.size(),
        .bindGroupLayouts = layouts.data(),
    };
    if (layoutMap.find(hash) != layoutMap.end()) {
        _gpuPipelineLayoutObj->wgpuPipelineLayout = static_cast<WGPUPipelineLayout>(layoutMap[hash]);
    } else {
        _gpuPipelineLayoutObj->wgpuPipelineLayout = wgpuDeviceCreatePipelineLayout(CCWGPUDevice::getInstance()->gpuDeviceObject()->wgpuDevice, &descriptor);
        layoutMap.emplace(hash, _gpuPipelineLayoutObj->wgpuPipelineLayout);
    }
}

void CCWGPUPipelineLayout::doDestroy() {
    if (_gpuPipelineLayoutObj) {
        if (_gpuPipelineLayoutObj->wgpuPipelineLayout) {
            wgpuPipelineLayoutRelease(_gpuPipelineLayoutObj->wgpuPipelineLayout);
        }
        delete _gpuPipelineLayoutObj;
    }
}

} // namespace gfx
} // namespace cc
