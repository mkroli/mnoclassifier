/*
 * MnoClassifier learns MSISDN-Operator combinations to afterwards predict Operators.
 * Copyright (C) 2013 MACH Connectivity GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.github.mkroli.mnoclassifier.persistence

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import resource.managed

trait Compression {
  self: PersistentTrainingDataFilesystemStore =>

  override def outputStream(file: File): OutputStream = new GZIPOutputStream(new FileOutputStream(file))

  override def inputStream(file: File): InputStream = new GZIPInputStream(new FileInputStream(file))
}

class PersistentTrainingDataFilesystemStore(file: File) {
  def outputStream(file: File): OutputStream = new FileOutputStream(file)

  def inputStream(file: File): InputStream = new FileInputStream(file)

  def store(f: (OutputStream) => Unit) {
    for (fos <- managed(outputStream(file))) {
      f(fos)
    }
  }

  def load[N](f: (InputStream) => N): N = {
    (for (fis <- managed(inputStream(file))) yield {
      f(fis)
    }).either match {
      case Left(t) => throw t.head
      case Right(r) => r
    }
  }
}
