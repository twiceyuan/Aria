/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
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
package com.arialyy.aria.core.upload;

import android.support.annotation.NonNull;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.command.AbsCmd;
import com.arialyy.aria.core.command.CmdFactory;
import com.arialyy.aria.core.download.DownloadReceiver;
import com.arialyy.aria.core.download.DownloadTaskEntity;
import com.arialyy.aria.core.inf.AbsReceiver;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.IReceiver;
import com.arialyy.aria.core.scheduler.DownloadSchedulers;
import com.arialyy.aria.core.scheduler.ISchedulerListener;
import com.arialyy.aria.core.scheduler.UploadSchedulers;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by lyy on 2017/2/6.
 * 上传功能接收器
 */
public class UploadReceiver extends AbsReceiver<UploadEntity> {
  private static final String TAG = "DownloadReceiver";
  public ISchedulerListener<UploadTask> listener;

  /**
   * 加载任务
   *
   * @param filePath 文件地址
   */
  public UploadTarget load(@NonNull String filePath) {
    CheckUtil.checkUploadPath(filePath);
    UploadEntity entity = UploadEntity.findData(UploadEntity.class, "filePath=?", filePath);
    if (entity == null) {
      entity = new UploadEntity();
    }
    String regex = "[/|\\\\|//]";
    Pattern p = Pattern.compile(regex);
    String[] strs = p.split(filePath);
    String fileName = strs[strs.length - 1];
    entity.setFileName(fileName);
    entity.setFilePath(filePath);
    return new UploadTarget(entity, targetName);
  }

  /**
   * 通过上传路径获取上传实体
   */
  public UploadEntity getUploadEntity(String filePath) {
    CheckUtil.checkUploadPath(filePath);
    return DbEntity.findData(UploadEntity.class, "filePath=?", filePath);
  }

  /**
   * 下载任务是否存在
   */
  @Override public boolean taskExists(String filePath) {
    return DbEntity.findData(UploadEntity.class, "filePath=?", filePath) != null;
  }

  @Override public List<UploadEntity> getTaskList() {
    return DbEntity.findAllData(UploadEntity.class);
  }

  @Override public void stopAllTask() {
    List<UploadEntity> allEntity = DbEntity.findAllData(UploadEntity.class);
    List<AbsCmd> stopCmds = new ArrayList<>();
    for (UploadEntity entity : allEntity) {
      if (entity.getState() == IEntity.STATE_RUNNING) {
        stopCmds.add(
            CommonUtil.createCmd(targetName, new UploadTaskEntity(entity), CmdFactory.TASK_STOP));
      }
    }
    AriaManager.getInstance(AriaManager.APP).setCmds(stopCmds).exe();
  }

  /**
   * 删除所有任务
   *
   * @param removeFile {@code true} 删除已经上传完成的任务，不仅删除上传记录，还会删除已经上传完成的文件，{@code false}
   * 如果文件已经上传完成，只删除上传记录
   */
  @Override public void removeAllTask(boolean removeFile) {
    final AriaManager am = AriaManager.getInstance(AriaManager.APP);

    AriaManager.getInstance(AriaManager.APP)
        .setCmd(
            CommonUtil.createCmd(targetName, new DownloadTaskEntity(), CmdFactory.TASK_CANCEL_ALL))
        .exe();

    Set<String> keys = am.getReceiver().keySet();
    for (String key : keys) {
      IReceiver receiver = am.getReceiver().get(key);
      receiver.removeSchedulerListener();
      am.getReceiver().remove(key);
    }
  }

  @Override public void destroy() {
    targetName = null;
    listener = null;
  }

  /**
   * 添加调度器回调
   *
   * @see #register()
   */
  @Deprecated public UploadReceiver addSchedulerListener(ISchedulerListener<UploadTask> listener) {
    this.listener = listener;
    UploadSchedulers.getInstance().addSchedulerListener(targetName, listener);
    return this;
  }

  @Override public void removeSchedulerListener() {
    if (listener != null) {
      UploadSchedulers.getInstance().removeSchedulerListener(targetName, listener);
    }
  }

  /**
   * 将当前类注册到Aria
   */
  public UploadReceiver register() {
    UploadSchedulers.getInstance().register(obj);
    return this;
  }

  @Override public void unRegister() {
    UploadSchedulers.getInstance().unRegister(obj);
  }
}