/*
 * Copyright 2014-2015 ieclipse.cn.
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
package com.mainaer.wjoklib.okhttp;

/**
 * REST url interface
 * 
 * @author Jamling
 * @date 2015年11月7日
 *
 */
public interface IUrl {
    /**
     * @return http method
     */
    int getMethod();

    /**
     * return the full url
     *
     * @return
     */
    String getUrl();

    /**
     * set query string to url.
     *
     * @param query query string, it a request parameter of string format usually
     */
    void setQuery(String query);
}
