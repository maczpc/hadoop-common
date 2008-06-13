/**
 * Copyright 2007 The Apache Software Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.io.Cell;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import org.apache.hadoop.hbase.io.BatchUpdate;
import org.apache.hadoop.hbase.util.Bytes;

/** Tests per-column bloom filters */
public class TestBloomFilters extends HBaseClusterTestCase {
  static final Log LOG = LogFactory.getLog(TestBloomFilters.class);

  private static final byte [] CONTENTS = Bytes.toBytes("contents:");

  private static final byte [][] rows = {
    Bytes.toBytes("wmjwjzyv"),
    Bytes.toBytes("baietibz"),
    Bytes.toBytes("guhsgxnv"),
    Bytes.toBytes("mhnqycto"),
    Bytes.toBytes("xcyqafgz"),
    Bytes.toBytes("zidoamgb"),
    Bytes.toBytes("tftfirzd"),
    Bytes.toBytes("okapqlrg"),
    Bytes.toBytes("yccwzwsq"),
    Bytes.toBytes("qmonufqu"),
    Bytes.toBytes("wlsctews"),
    Bytes.toBytes("mksdhqri"),
    Bytes.toBytes("wxxllokj"),
    Bytes.toBytes("eviuqpls"),
    Bytes.toBytes("bavotqmj"),
    Bytes.toBytes("yibqzhdl"),
    Bytes.toBytes("csfqmsyr"),
    Bytes.toBytes("guxliyuh"),
    Bytes.toBytes("pzicietj"),
    Bytes.toBytes("qdwgrqwo"),
    Bytes.toBytes("ujfzecmi"),
    Bytes.toBytes("dzeqfvfi"),
    Bytes.toBytes("phoegsij"),
    Bytes.toBytes("bvudfcou"),
    Bytes.toBytes("dowzmciz"),
    Bytes.toBytes("etvhkizp"),
    Bytes.toBytes("rzurqycg"),
    Bytes.toBytes("krqfxuge"),
    Bytes.toBytes("gflcohtd"),
    Bytes.toBytes("fcrcxtps"),
    Bytes.toBytes("qrtovxdq"),
    Bytes.toBytes("aypxwrwi"),
    Bytes.toBytes("dckpyznr"),
    Bytes.toBytes("mdaawnpz"),
    Bytes.toBytes("pakdfvca"),
    Bytes.toBytes("xjglfbez"),
    Bytes.toBytes("xdsecofi"),
    Bytes.toBytes("sjlrfcab"),
    Bytes.toBytes("ebcjawxv"),
    Bytes.toBytes("hkafkjmy"),
    Bytes.toBytes("oimmwaxo"),
    Bytes.toBytes("qcuzrazo"),
    Bytes.toBytes("nqydfkwk"),
    Bytes.toBytes("frybvmlb"),
    Bytes.toBytes("amxmaqws"),
    Bytes.toBytes("gtkovkgx"),
    Bytes.toBytes("vgwxrwss"),
    Bytes.toBytes("xrhzmcep"),
    Bytes.toBytes("tafwziil"),
    Bytes.toBytes("erjmncnv"),
    Bytes.toBytes("heyzqzrn"),
    Bytes.toBytes("sowvyhtu"),
    Bytes.toBytes("heeixgzy"),
    Bytes.toBytes("ktcahcob"),
    Bytes.toBytes("ljhbybgg"),
    Bytes.toBytes("jiqfcksl"),
    Bytes.toBytes("anjdkjhm"),
    Bytes.toBytes("uzcgcuxp"),
    Bytes.toBytes("vzdhjqla"),
    Bytes.toBytes("svhgwwzq"),
    Bytes.toBytes("zhswvhbp"),
    Bytes.toBytes("ueceybwy"),
    Bytes.toBytes("czkqykcw"),
    Bytes.toBytes("ctisayir"),
    Bytes.toBytes("hppbgciu"),
    Bytes.toBytes("nhzgljfk"),
    Bytes.toBytes("vaziqllf"),
    Bytes.toBytes("narvrrij"),
    Bytes.toBytes("kcevbbqi"),
    Bytes.toBytes("qymuaqnp"),
    Bytes.toBytes("pwqpfhsr"),
    Bytes.toBytes("peyeicuk"),
    Bytes.toBytes("kudlwihi"),
    Bytes.toBytes("pkmqejlm"),
    Bytes.toBytes("ylwzjftl"),
    Bytes.toBytes("rhqrlqar"),
    Bytes.toBytes("xmftvzsp"),
    Bytes.toBytes("iaemtihk"),
    Bytes.toBytes("ymsbrqcu"),
    Bytes.toBytes("yfnlcxto"),
    Bytes.toBytes("nluqopqh"),
    Bytes.toBytes("wmrzhtox"),
    Bytes.toBytes("qnffhqbl"),
    Bytes.toBytes("zypqpnbw"),
    Bytes.toBytes("oiokhatd"),
    Bytes.toBytes("mdraddiu"),
    Bytes.toBytes("zqoatltt"),
    Bytes.toBytes("ewhulbtm"),
    Bytes.toBytes("nmswpsdf"),
    Bytes.toBytes("xsjeteqe"),
    Bytes.toBytes("ufubcbma"),
    Bytes.toBytes("phyxvrds"),
    Bytes.toBytes("vhnfldap"),
    Bytes.toBytes("zrrlycmg"),
    Bytes.toBytes("becotcjx"),
    Bytes.toBytes("wvbubokn"),
    Bytes.toBytes("avkgiopr"),
    Bytes.toBytes("mbqqxmrv"),
    Bytes.toBytes("ibplgvuu"),
    Bytes.toBytes("dghvpkgc")
  };

  private static final byte [][] testKeys = {
      Bytes.toBytes("abcdefgh"),
      Bytes.toBytes("ijklmnop"),
      Bytes.toBytes("qrstuvwx"),
      Bytes.toBytes("yzabcdef")
  };
  
  /** constructor */
  public TestBloomFilters() {
    super();
  }
  
  /**
   * Test that specifies explicit parameters for the bloom filter
   * @throws IOException
   */
  public void testExplicitParameters() throws IOException {
    HTable table = null;

    // Setup
    
    HTableDescriptor desc = new HTableDescriptor(getName());
    BloomFilterDescriptor bloomFilter =
      new BloomFilterDescriptor(              // if we insert 1000 values
          BloomFilterDescriptor.BloomFilterType.BLOOMFILTER,  // plain old bloom filter
          12499,                              // number of bits
          4                                   // number of hash functions
      );

    desc.addFamily(
        new HColumnDescriptor(CONTENTS,               // Column name
            1,                                        // Max versions
            HColumnDescriptor.CompressionType.NONE,   // no compression
            HColumnDescriptor.DEFAULT_IN_MEMORY,      // not in memory
            HColumnDescriptor.DEFAULT_BLOCKCACHE,
            HColumnDescriptor.DEFAULT_MAX_VALUE_LENGTH,
            HColumnDescriptor.DEFAULT_TTL,
            bloomFilter
        )
    );

    // Create the table

    HBaseAdmin admin = new HBaseAdmin(conf);
    admin.createTable(desc);

    // Open table

    table = new HTable(conf, desc.getName());

    // Store some values

    for(int i = 0; i < 100; i++) {
      byte [] row = rows[i];
      String value = row.toString();
      BatchUpdate b = new BatchUpdate(row);
      b.put(CONTENTS, value.getBytes(HConstants.UTF8_ENCODING));
      table.commit(b);
    }
    try {
      // Give cache flusher and log roller a chance to run
      // Otherwise we'll never hit the bloom filter, just the memcache
      Thread.sleep(conf.getLong(HConstants.THREAD_WAKE_FREQUENCY, 10 * 1000) * 2);
    } catch (InterruptedException e) {
      // ignore
    }

    
    for(int i = 0; i < testKeys.length; i++) {
      Cell value = table.get(testKeys[i], CONTENTS);
      if(value != null && value.getValue().length != 0) {
        LOG.info("non existant key: " + testKeys[i] + " returned value: " +
            new String(value.getValue(), HConstants.UTF8_ENCODING));
      }
    }
  }
  
  /**
   * Test that uses computed for the bloom filter
   * @throws IOException
   */
  public void testComputedParameters() throws IOException {
    HTable table = null;

    // Setup
    
    HTableDescriptor desc = new HTableDescriptor(getName());
      
    BloomFilterDescriptor bloomFilter =
      new BloomFilterDescriptor(
          BloomFilterDescriptor.BloomFilterType.BLOOMFILTER,  // plain old bloom filter
          1000                                  // estimated number of entries
      );
    LOG.info("vector size: " + bloomFilter.vectorSize);

    desc.addFamily(
        new HColumnDescriptor(CONTENTS,               // Column name
            1,                                        // Max versions
            HColumnDescriptor.CompressionType.NONE,   // no compression
            HColumnDescriptor.DEFAULT_IN_MEMORY,      // not in memory
            HColumnDescriptor.DEFAULT_BLOCKCACHE,
            HColumnDescriptor.DEFAULT_MAX_VALUE_LENGTH,
            HColumnDescriptor.DEFAULT_TTL,
            bloomFilter
        )
    );

    // Create the table

    HBaseAdmin admin = new HBaseAdmin(conf);
    admin.createTable(desc);

    // Open table

    table = new HTable(conf, desc.getName());

    // Store some values

    for(int i = 0; i < 100; i++) {
      byte [] row = rows[i];
      String value = row.toString();
      BatchUpdate b = new BatchUpdate(row);
      b.put(CONTENTS, value.getBytes(HConstants.UTF8_ENCODING));
      table.commit(b);
    }
    try {
      // Give cache flusher and log roller a chance to run
      // Otherwise we'll never hit the bloom filter, just the memcache
      Thread.sleep(conf.getLong(HConstants.THREAD_WAKE_FREQUENCY, 10 * 1000) * 2);
      
    } catch (InterruptedException e) {
      // ignore
    }
    
    for(int i = 0; i < testKeys.length; i++) {
      Cell value = table.get(testKeys[i], CONTENTS);
      if(value != null && value.getValue().length != 0) {
        LOG.info("non existant key: " + testKeys[i] + " returned value: " +
            new String(value.getValue(), HConstants.UTF8_ENCODING));
      }
    }
  }
}
