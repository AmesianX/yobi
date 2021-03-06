/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2013 NAVER Corp.
 * http://yobi.io
 *
 * @Author Wansoon Park, kjkmadness
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package playRepository.hooks;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import models.Project;
import models.PullRequest;
import models.PushedBranch;

import org.eclipse.jgit.transport.PostReceiveHook;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceiveCommand.Type;
import org.eclipse.jgit.transport.ReceivePack;

import utils.JodaDateUtil;

/**
 * 프로젝트의 가장 최근 Push된 브랜치 저장
 */
public class UpdateRecentlyPushedBranch implements PostReceiveHook {
    private Project project;

    public UpdateRecentlyPushedBranch(Project project) {
        this.project = project;
    }

    @Override
    public void onPostReceive(ReceivePack receivePack, Collection<ReceiveCommand> commands) {
        removeOldPushedBranches();
        saveRecentlyPushedBranch(getPushedBranches(commands));
    }

    /*
     * 오래전 푸쉬된 브랜치를 삭제한다.
     */
    private void removeOldPushedBranches() {
        List<PushedBranch> list = project.getOldPushedBranches();
        for (PushedBranch pushedBranch : list) {
            pushedBranch.delete();
        }
    }

    /*
     * 최근 푸쉬된 브랜치를 저장한다.
     * 이미 존재할 경우 {@code pushedDate}만 업데이트한다.
     * 푸쉬된 브랜치를 보내는 코드(열림/보류 상태)가 있으면 저장하지 않는다.
     */
    private void saveRecentlyPushedBranch(Set<String> pushedBranches) {
        for (String branch : pushedBranches) {
            PushedBranch pushedBranch = PushedBranch.find.where()
                            .eq("project", project).eq("name", branch).findUnique();

            if (pushedBranch != null) {
                pushedBranch.pushedDate = JodaDateUtil.now();
                pushedBranch.update();
            }

            if (pushedBranch == null && PullRequest.findByFromProjectAndBranch(project, branch).isEmpty()) {
                pushedBranch = new PushedBranch(JodaDateUtil.now(), branch, project);
                pushedBranch.save();
            }
        }
    }

    /*
     * ReceiveCommand 중, branch create, update 에 해당하는 것들의 참조 branch set 을 구한다.
     */
    private Set<String> getPushedBranches(
            Collection<ReceiveCommand> commands) {
        return ReceiveCommandUtil.getRefNamesByCommandType(commands,
                Type.CREATE,
                Type.UPDATE,
                Type.UPDATE_NONFASTFORWARD);
    }
}
