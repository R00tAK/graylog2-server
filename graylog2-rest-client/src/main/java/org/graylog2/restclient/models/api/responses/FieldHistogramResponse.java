/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.api.responses;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FieldHistogramResponse extends HistogramResponse {

    public Map<String, Map<String, Object>> results;

    /**
     * [{ x: -1893456000, y: 92228531 }, { x: -1577923200, y: 106021568 }]
     *
     * @return A JSON map representation of the result, suitable for Rickshaw data graphing.
     */
    public List<Map<String, Object>> getFormattedResults(String value) {
        List<Map<String, Object>> points = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> result : results.entrySet()) {
            Map<String, Object> point = Maps.newHashMap();
            point.put("x", Long.parseLong(result.getKey()));
            point.put("y", result.getValue().get(value));

            points.add(point);
        }

        return points;
    }

}
