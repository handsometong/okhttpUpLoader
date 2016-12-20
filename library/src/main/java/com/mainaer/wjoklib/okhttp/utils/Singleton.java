/*
 * Copyright 2014-2016 wjokhttp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mainaer.wjoklib.okhttp.utils;

/**
 * 类/接口描述
 *
 * @author wangjian
 * @date 2016/5/13 .
 */
public class Singleton {

    private static Singleton instance = null;

    /**
     * 方法加锁，防止多线程操作时出现多个实例
     */
    private static synchronized void init() {
        if (instance == null) {
            instance = new Singleton();
        }
    }

    /**
     * 获得当前对象实例
     *
     * @param <T>
     * @return
     */
    public final static <T extends Singleton> T getInstance() {
        if (instance == null) {
            init();
        }
        return (T) instance;
    }

}
