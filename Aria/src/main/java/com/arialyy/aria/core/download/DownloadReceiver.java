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
package com.arialyy.aria.core.download;

import android.support.annotation.NonNull;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.inf.AbsReceiver;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.IReceiver;
import com.arialyy.aria.core.command.CmdFactory;
import com.arialyy.aria.core.command.AbsCmd;
import com.arialyy.aria.core.scheduler.DownloadSchedulers;
import com.arialyy.aria.core.scheduler.ISchedulerListener;
import com.arialyy.aria.orm.DbEntity;
import com.arialyy.aria.util.CheckUtil;
import com.arialyy.aria.util.CommonUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by lyy on 2016/12/5.
 * 下载功能接收器
 */
public class DownloadReceiver extends AbsReceiver<DownloadEntity> {
  private static final String TAG = "DownloadReceiver";
  public ISchedulerListener<DownloadTask> listener;

  /**
   * 设置最大下载速度，单位：kb
   * 该方法为实验性功能，清不要轻易在生产环境中使用。
   *
   * @param maxSpeed 为0表示不限速
   */
  @Deprecated public void setMaxSpeed(double maxSpeed) {
    AriaManager.getInstance(AriaManager.APP).getDownloadConfig().setMsxSpeed(maxSpeed);
  }

  /**
   * {@link #load(String)}，请使用该方法
   */
  @Deprecated public DownloadTarget load(DownloadEntity entity) {
    return new DownloadTarget(entity, targetName);
  }

  /**
   * 加载下载地址
   */
  public DownloadTarget load(@NonNull String downloadUrl) {
    CheckUtil.checkDownloadUrl(downloadUrl);
    DownloadEntity entity =
        DownloadEntity.findData(DownloadEntity.class, "downloadUrl=?", downloadUrl);
    if (entity == null) {
      entity = new DownloadEntity();
    }
    File file = new File(entity.getDownloadPath());
    if (!file.exists()) {
      entity.setState(IEntity.STATE_WAIT);
    }
    entity.setDownloadUrl(downloadUrl);
    return new DownloadTarget(entity, targetName);
  }

  /**
   * 添加调度器回调
   *
   * @see #register()
   */
  @Deprecated public DownloadReceiver addSchedulerListener(
      ISchedulerListener<DownloadTask> listener) {
    this.listener = listener;
    DownloadSchedulers.getInstance().addSchedulerListener(targetName, listener);
    return this;
  }

  /**
   * 将当前类注册到Aria
   */
  public DownloadReceiver register() {
    DownloadSchedulers.getInstance().register(obj);
    return this;
  }

  /**
   * 取消注册
   */
  @Override public void unRegister() {
    DownloadSchedulers.getInstance().unRegister(obj);
  }

  /**
   * 移除回调
   *
   * @see #unRegister()
   */
  @Deprecated @Override public void removeSchedulerListener() {
    if (listener != null) {
      DownloadSchedulers.getInstance().removeSchedulerListener(targetName, listener);
    }
  }

  @Override public void destroy() {
    targetName = null;
    listener = null;
  }

  /**
   * 通过下载链接获取下载实体
   */
  public DownloadEntity getDownloadEntity(String downloadUrl) {
    CheckUtil.checkDownloadUrl(downloadUrl);
    return DownloadEntity.findData(DownloadEntity.class, "downloadUrl=?", downloadUrl);
  }

  /**
   * 下载任务是否存在
   */
  @Override public boolean taskExists(String downloadUrl) {
    return DownloadEntity.findData(DownloadEntity.class, "downloadUrl=?", downloadUrl) != null;
  }

  @Override public List<DownloadEntity> getTaskList() {
    return DownloadEntity.findAllData(DownloadEntity.class);
  }

  /**
   * 停止所有正在下载的任务，并清空等待队列。
   */
  @Override public void stopAllTask() {
    AriaManager.getInstance(AriaManager.APP)
        .setCmd(CmdFactory.getInstance()
            .createCmd(targetName, new DownloadTaskEntity(), CmdFactory.TASK_STOP_ALL))
        .exe();
  }

  /**
   * 恢复所有正在下载的任务
   * 1.如果执行队列没有满，则开始下载任务，直到执行队列满
   * 2.如果队列执行队列已经满了，则将所有任务添加到等待队列中
   */
  public void resumeAllTask() {
    AriaManager.getInstance(AriaManager.APP)
        .setCmd(CmdFactory.getInstance()
            .createCmd(targetName, new DownloadTaskEntity(), CmdFactory.TASK_RESUME_ALL))
        .exe();
  }

  /**
   * 删除所有任务
   *
   * @param removeFile {@code true} 删除已经下载完成的任务，不仅删除下载记录，还会删除已经下载完成的文件，{@code false}
   * 如果文件已经下载完成，只删除下载记录
   */
  @Override public void removeAllTask(boolean removeFile) {
    final AriaManager ariaManager = AriaManager.getInstance(AriaManager.APP);
    AriaManager.getInstance(AriaManager.APP)
        .setCmd(
            CommonUtil.createCmd(targetName, new DownloadTaskEntity(), CmdFactory.TASK_CANCEL_ALL))
        .exe();

    Set<String> keys = ariaManager.getReceiver().keySet();
    for (String key : keys) {
      IReceiver receiver = ariaManager.getReceiver().get(key);
      receiver.removeSchedulerListener();
      receiver.unRegister();
      ariaManager.getReceiver().remove(key);
    }
  }
}