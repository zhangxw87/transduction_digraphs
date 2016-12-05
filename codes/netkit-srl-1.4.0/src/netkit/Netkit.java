/**
 * Netkit.java
 * Copyright (C) 2008 Sofus A. Macskassy
 *
 * Part of the open-source Network Learning Toolkit
 * http://netkit-srl.sourceforge.net
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
 *
 **/

package netkit;

import netkit.classifiers.NetworkLearning;

public class Netkit {

  public static final String learning = "learning";
  public static final String edgetransform = "edgetransform";
  public static final String textgraph = "textgraph";
  public static final String graphstat = "graphstat";
  public static final String convert = "convert";

  public static void usage() {
    System.out.println("usage: NetKit -h");
    for(String cmd : NetworkLearning.getCommandLines())
      System.out.println(cmd);
    for(String cmd : EdgeTransformer.getCommandLines())
      System.out.println(cmd);
    for(String cmd : GraphStat.getCommandLines())
      System.out.println(cmd);
    System.exit(0);
  }

  public static void main(String[] argv) {
    if(argv.length == 0 || argv[0].toLowerCase().startsWith("-h"))
      usage();
    if(argv[0].equalsIgnoreCase(edgetransform)) {
      EdgeTransformer.run(argv);
    } else if(argv[0].equalsIgnoreCase(graphstat)) {
      GraphStat.run(argv);
    } else if(argv[0].equalsIgnoreCase(textgraph)) {
      // create a greph from text
    } else if(argv[0].equalsIgnoreCase(convert)) {
      // CONVERT BETWEEN NETKIT AND OTHER FORMATS
    } else if(argv[0].equalsIgnoreCase(learning)) {
      NetworkLearning.run(argv);
    } else {
      NetworkLearning.run(argv);
    }
  }
}
