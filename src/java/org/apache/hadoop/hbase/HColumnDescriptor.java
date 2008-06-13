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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;

/**
 * An HColumnDescriptor contains information about a column family such as the
 * number of versions, compression settings, etc.
 * 
 * It is used as input when creating a table or adding a column. Once set, the
 * parameters that specify a column cannot be changed without deleting the
 * column and recreating it. If there is data stored in the column, it will be
 * deleted when the column is deleted.
 */
public class HColumnDescriptor implements WritableComparable {
  // For future backward compatibility

  // Version 3 was when column names becaome byte arrays and when we picked up
  // Time-to-live feature.  Version 4 was when we moved to byte arrays, HBASE-82.
  private static final byte COLUMN_DESCRIPTOR_VERSION = (byte)4;

  /** 
   * The type of compression.
   * @see org.apache.hadoop.io.SequenceFile.Writer
   */
  public static enum CompressionType {
    /** Do not compress records. */
    NONE, 
    /** Compress values only, each separately. */
    RECORD,
    /** Compress sequences of records together in blocks. */
    BLOCK
  }

  // Defines for jruby/shell
  public static final String NAME = "NAME";
  public static final String MAX_VERSIONS = "MAX_VERSIONS";
  public static final String COMPRESSION = "COMPRESSION";
  public static final String IN_MEMORY = "IN_MEMORY";
  public static final String BLOCKCACHE = "BLOCKCACHE";
  public static final String MAX_LENGTH = "MAX_LENGTH";
  public static final String TTL = "TTL";
  public static final String BLOOMFILTER = "BLOOMFILTER";
  public static final String FOREVER = "FOREVER";

  /**
   * Default compression type.
   */
  public static final CompressionType DEFAULT_COMPRESSION =
    CompressionType.NONE;

  /**
   * Default number of versions of a record to keep.
   */
  public static final int DEFAULT_MAX_VERSIONS = 3;

  /**
   * Default maximum cell length.
   */
  public static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;

  /**
   * Default setting for whether to serve from memory or not.
   */
  public static final boolean DEFAULT_IN_MEMORY = false;

  /**
   * Default setting for whether to use a block cache or not.
   */
  public static final boolean DEFAULT_BLOCKCACHE = false;

  /**
   * Default maximum length of cell contents.
   */
  public static final int DEFAULT_MAX_VALUE_LENGTH = Integer.MAX_VALUE;

  /**
   * Default time to live of cell contents.
   */
  public static final int DEFAULT_TTL = HConstants.FOREVER;

  /**
   * Default bloom filter description.
   */
  public static final BloomFilterDescriptor DEFAULT_BLOOMFILTER = null;

  // Column family name
  private byte [] name;
  // Number of versions to keep
  private int maxVersions = DEFAULT_MAX_VERSIONS;
  // Compression setting if any
  private CompressionType compressionType = DEFAULT_COMPRESSION;
  // Serve reads from in-memory cache
  private boolean inMemory = DEFAULT_IN_MEMORY;
  // Serve reads from in-memory block cache
  private boolean blockCacheEnabled = DEFAULT_BLOCKCACHE;
  // Maximum value size
  private int maxValueLength = DEFAULT_MAX_LENGTH;
  // Time to live of cell contents, in seconds from last timestamp
  private int timeToLive = DEFAULT_TTL;
  // True if bloom filter was specified
  private boolean bloomFilterSpecified = false;
  // Descriptor of bloom filter
  private BloomFilterDescriptor bloomFilter = DEFAULT_BLOOMFILTER;

  /**
   * Default constructor. Must be present for Writable.
   */
  public HColumnDescriptor() {
    this.name = null;
  }

  /**
   * Construct a column descriptor specifying only the family name 
   * The other attributes are defaulted.
   * 
   * @param columnName - column family name
   */
  public HColumnDescriptor(final String columnName) {
    this(Bytes.toBytes(columnName));
  }

  /**
   * Construct a column descriptor specifying only the family name 
   * The other attributes are defaulted.
   * 
   * @param columnName - column family name
   */
  public HColumnDescriptor(final Text columnName) {
    this(columnName.getBytes());
  }
  
  /**
   * Construct a column descriptor specifying only the family name 
   * The other attributes are defaulted.
   * 
   * @param columnName Column family name.  Must have the ':' ending.
   */
  public HColumnDescriptor(final byte [] columnName) {
    this (columnName == null || columnName.length <= 0?
      HConstants.EMPTY_BYTE_ARRAY: columnName,
      DEFAULT_MAX_VERSIONS, DEFAULT_COMPRESSION, DEFAULT_IN_MEMORY,
      DEFAULT_BLOCKCACHE, 
      Integer.MAX_VALUE, DEFAULT_TTL,
      DEFAULT_BLOOMFILTER);
  }

  /**
   * Constructor
   * @param columnName Column family name.  Must have the ':' ending.
   * @param maxVersions Maximum number of versions to keep
   * @param compression Compression type
   * @param inMemory If true, column data should be kept in an HRegionServer's
   * cache
   * @param blockCacheEnabled If true, MapFile blocks should be cached
   * @param maxValueLength Restrict values to &lt;= this value
   * @param timeToLive Time-to-live of cell contents, in seconds from last timestamp
   * (use HConstants.FOREVER for unlimited TTL)
   * @param bloomFilter Enable the specified bloom filter for this column
   * 
   * @throws IllegalArgumentException if passed a family name that is made of 
   * other than 'word' characters: i.e. <code>[a-zA-Z_0-9]</code> and does not
   * end in a <code>:</code>
   * @throws IllegalArgumentException if the number of versions is &lt;= 0
   */
  public HColumnDescriptor(final byte [] columnName, final int maxVersions,
      final CompressionType compression, final boolean inMemory,
      final boolean blockCacheEnabled,
      final int maxValueLength, final int timeToLive,
      final BloomFilterDescriptor bloomFilter) {
    isLegalFamilyName(columnName);
    this.name = stripColon(columnName);
    if (maxVersions <= 0) {
      // TODO: Allow maxVersion of 0 to be the way you say "Keep all versions".
      // Until there is support, consider 0 or < 0 -- a configuration error.
      throw new IllegalArgumentException("Maximum versions must be positive");
    }
    this.maxVersions = maxVersions;
    this.inMemory = inMemory;
    this.blockCacheEnabled = blockCacheEnabled;
    this.maxValueLength = maxValueLength;
    this.timeToLive = timeToLive;
    this.bloomFilter = bloomFilter;
    this.bloomFilterSpecified = this.bloomFilter == null ? false : true;
    this.compressionType = compression;
  }
  
  private static byte [] stripColon(final byte [] n) {
    byte [] result = new byte [n.length - 1];
    // Have the stored family name be absent the colon delimiter
    System.arraycopy(n, 0, result, 0, n.length - 1);
    return result;
  }
  
  /**
   * @param b Family name.
   * @return <code>b</code>
   * @throws IllegalArgumentException If not null and not a legitimate family
   * name: i.e. 'printable' and ends in a ':' (Null passes are allowed because
   * <code>b</code> can be null when deserializing).
   */
  public static byte [] isLegalFamilyName(final byte [] b) {
    if (b == null) {
      return b;
    }
    if (b[b.length - 1] != ':') {
      throw new IllegalArgumentException("Family names must end in a colon: " +
        Bytes.toString(b));
    }
    for (int i = 0; i < (b.length - 1); i++) {
      if (Character.isLetterOrDigit(b[i]) || b[i] == '_' || b[i] == '.') {
        continue;
      }
      throw new IllegalArgumentException("Illegal character <" + b[i] +
        ">. Family names  can only contain  'word characters' and must end" +
        "with a colon: " + Bytes.toString(b));
    }
    return b;
  }

  /**
   * @return Name of this column family
   */
  public byte [] getName() {
    return name;
  }

  /**
   * @return Name of this column family
   */
  public String getNameAsString() {
    return Bytes.toString(this.name);
  }

  /** @return compression type being used for the column family */
  public CompressionType getCompression() {
    return this.compressionType;
  }
  
  /** @return maximum number of versions */
  public int getMaxVersions() {
    return this.maxVersions;
  }
  
  /**
   * @return Compression type setting.
   */
  public CompressionType getCompressionType() {
    return this.compressionType;
  }

  /**
   * @return True if we are to keep all in use HRegionServer cache.
   */
  public boolean isInMemory() {
    return this.inMemory;
  }
  
  /**
   * @return Maximum value length.
   */
  public int getMaxValueLength() {
    return this.maxValueLength;
  }

  /**
   * @return Time to live.
   */
  public int getTimeToLive() {
    return this.timeToLive;
  }

  /**
   * @return True if MapFile blocks should be cached.
   */
  public boolean isBlockCacheEnabled() {
    return blockCacheEnabled;
  }

  /**
   * @return Bloom filter descriptor or null if none set.
   */
  public BloomFilterDescriptor getBloomFilter() {
    return this.bloomFilter;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "{" + NAME + " => '" + Bytes.toString(name) +
      "', " + MAX_VERSIONS + " => " + maxVersions +
      ", " + COMPRESSION + " => '" + this.compressionType +
      "', " + IN_MEMORY + " => " + inMemory +
      ", " + BLOCKCACHE + " => " + blockCacheEnabled +
      ", " + MAX_LENGTH + " => " + maxValueLength +
      ", " + TTL + " => " +
          (timeToLive == HConstants.FOREVER ? "FOREVER" : 
              Integer.toString(timeToLive)) +
      ", " + BLOOMFILTER + " => " +
        (bloomFilterSpecified ? bloomFilter.toString() : CompressionType.NONE) +
      "}";
  }
  
  /** {@inheritDoc} */
  @Override
  public boolean equals(Object obj) {
    return compareTo(obj) == 0;
  }
  
  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    int result = Bytes.hashCode(this.name);
    result ^= Integer.valueOf(this.maxVersions).hashCode();
    result ^= this.compressionType.hashCode();
    result ^= Boolean.valueOf(this.inMemory).hashCode();
    result ^= Boolean.valueOf(this.blockCacheEnabled).hashCode();
    result ^= Integer.valueOf(this.maxValueLength).hashCode();
    result ^= Integer.valueOf(this.timeToLive).hashCode();
    result ^= Boolean.valueOf(this.bloomFilterSpecified).hashCode();
    result ^= Byte.valueOf(COLUMN_DESCRIPTOR_VERSION).hashCode();
    if (this.bloomFilterSpecified) {
      result ^= this.bloomFilter.hashCode();
    }
    return result;
  }
  
  // Writable

  /** {@inheritDoc} */
  public void readFields(DataInput in) throws IOException {
    int versionNumber = in.readByte();
    if (versionNumber <= 2) {
      Text t = new Text();
      t.readFields(in);
      this.name = t.getBytes();
      if (HStoreKey.getFamilyDelimiterIndex(this.name) > 0) {
        this.name = stripColon(this.name);
      }
    } else {
      this.name = Bytes.readByteArray(in);
    }
    this.maxVersions = in.readInt();
    int ordinal = in.readInt();
    this.compressionType = CompressionType.values()[ordinal];
    this.inMemory = in.readBoolean();
    this.maxValueLength = in.readInt();
    this.bloomFilterSpecified = in.readBoolean();
    
    if(bloomFilterSpecified) {
      bloomFilter = new BloomFilterDescriptor();
      bloomFilter.readFields(in);
    }
    
    if (versionNumber > 1) {
      this.blockCacheEnabled = in.readBoolean();
    }

    if (versionNumber > 2) {
      this.timeToLive = in.readInt();
    }
  }

  /** {@inheritDoc} */
  public void write(DataOutput out) throws IOException {
    out.writeByte(COLUMN_DESCRIPTOR_VERSION);
    Bytes.writeByteArray(out, this.name);
    out.writeInt(this.maxVersions);
    out.writeInt(this.compressionType.ordinal());
    out.writeBoolean(this.inMemory);
    out.writeInt(this.maxValueLength);
    out.writeBoolean(this.bloomFilterSpecified);
    
    if(bloomFilterSpecified) {
      bloomFilter.write(out);
    }
    out.writeBoolean(this.blockCacheEnabled);
    out.writeInt(this.timeToLive);
  }

  // Comparable

  /** {@inheritDoc} */
  public int compareTo(Object o) {
    HColumnDescriptor other = (HColumnDescriptor)o;
    int result = Bytes.compareTo(this.name, other.getName());
    if(result == 0) {
      result = Integer.valueOf(this.maxVersions).compareTo(
          Integer.valueOf(other.maxVersions));
    }
    
    if(result == 0) {
      result = this.compressionType.compareTo(other.compressionType);
    }
    
    if(result == 0) {
      if(this.inMemory == other.inMemory) {
        result = 0;
        
      } else if(this.inMemory) {
        result = -1;
        
      } else {
        result = 1;
      }
    }
    
    if(result == 0) {
      if(this.blockCacheEnabled == other.blockCacheEnabled) {
        result = 0;
        
      } else if(this.blockCacheEnabled) {
        result = -1;
        
      } else {
        result = 1;
      }
    }
    
    if(result == 0) {
      result = other.maxValueLength - this.maxValueLength;
    }

    if(result == 0) {
      result = other.timeToLive - this.timeToLive;
    }

    if(result == 0) {
      if(this.bloomFilterSpecified == other.bloomFilterSpecified) {
        result = 0;
        
      } else if(this.bloomFilterSpecified) {
        result = -1;
        
      } else {
        result = 1;
      }
    }
    
    if(result == 0 && this.bloomFilterSpecified) {
      result = this.bloomFilter.compareTo(other.bloomFilter);
    }
    
    return result;
  }
}