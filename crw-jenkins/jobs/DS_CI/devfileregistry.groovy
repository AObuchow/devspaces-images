import groovy.json.JsonSlurper

def curlCMD = "https://raw.githubusercontent.com/redhat-developer/devspaces/devspaces-3-rhel-8/dependencies/job-config.json".toURL().text

def jsonSlurper = new JsonSlurper();
def config = jsonSlurper.parseText(curlCMD);

def JOB_BRANCHES = config.Jobs.devfileregistry?.keySet()
for (JB in JOB_BRANCHES) {
    //check for jenkinsfile
    FILE_CHECK = false
    try {
        fileCheck = readFileFromWorkspace('jobs/DS_CI/template_'+JB+'.jenkinsfile')
        FILE_CHECK = true
    }
    catch(err) {
        println "No jenkins file found for " + JB
    }
    if (FILE_CHECK) {
        JOB_BRANCH=""+JB
        MIDSTM_BRANCH="devspaces-" + JOB_BRANCH.replaceAll(".x","") + "-rhel-8"
        jobPath="${FOLDER_PATH}/${ITEM_NAME}_" + JOB_BRANCH
        pipelineJob(jobPath){
            disabled(config.Jobs.devfileregistry[JB].disabled) // on reload of job, disable to avoid churn
            UPSTM_NAME="che-devfile-registry"
            MIDSTM_NAME="devfileregistry"
            SOURCE_REPO_CHE="eclipse-che/" + UPSTM_NAME
            SOURCE_REPO="redhat-developer/devspaces"
            MIDSTM_REPO="redhat-developer/devspaces-images"

            def CMD = "git ls-remote --heads https://github.com/" + SOURCE_REPO + ".git " + config.Jobs.devfileregistry[JB].upstream_branch[0]
            def BRANCH_CHECK=CMD.execute().text

            SOURCE_BRANCH=""+config.Jobs.devfileregistry[JB].upstream_branch[0]
            if (!BRANCH_CHECK) {
                //devfileregistry uses devspaces-3.yy-rhel-8 so if branch doesn't exist use devspaces-3-rhel-8
                SOURCE_BRANCH="devspaces-3-rhel-8"
            }

            description('''
Artifact builder + sync job; triggers brew after syncing

<ul>
<li>Upstream Che: <a href=https://github.com/''' + SOURCE_REPO_CHE + '''>''' + UPSTM_NAME + '''</a></li>
<li>Upstream Dev Spaces: <a href=https://github.com/''' + SOURCE_REPO + '''/tree/''' + MIDSTM_BRANCH + '''/dependencies/''' + UPSTM_NAME + '''/>''' + UPSTM_NAME + '''</a></li>
<li>Midstream: <a href=https://github.com/''' + MIDSTM_REPO + '''/tree/''' + MIDSTM_BRANCH + '''/devspaces-''' + MIDSTM_NAME + '''/>devspaces-''' + MIDSTM_NAME + '''</a></li>
<li>Downstream: <a href=https://pkgs.devel.redhat.com/cgit/containers/devspaces-''' + MIDSTM_NAME + '''?h=''' + MIDSTM_BRANCH + '''>''' + MIDSTM_NAME + '''</a></li>
</ul>

<p>This job will be <a href=https://issues.redhat.com/browse/CRW-3178>triggered</a>:
<ul>
<li>for changes to 
    <a href=https://github.com/redhat-developer/devspaces/blob/devspaces-3-rhel-8/dependencies/che-devfile-registry>devspaces/che-devfile-registry</a></li>
<li>for 
    <a href=https://github.com/devspaces-samples/nodejs-mongodb-sample/blob/devspaces-3-rhel-8/.github/workflows/rebuild-devfile-registry.yml>changes</a>
    to the <a href=https://github.com/devspaces-samples/>devspaces-samples</a> repos 
    used by the <a href=https://github.com/redhat-developer/devspaces/tree/devspaces-3-rhel-8/dependencies/che-devfile-registry/devfiles>devfile registry</a> (see <a href=https://github.com/redhat-developer/devspaces/commits/devspaces-3-rhel-8/dependencies/che-devfile-registry/webhook_trigger.txt>webhook_trigger.txt</a>)</li>
<li>for every new <a href=../pluginregistry_''' + JOB_BRANCH + '''/>plugin registry</a> container <a href=https://quay.io/repository/devspaces/pluginregistry-rhel8?tab=tags>image</a> that is
    <a href=https://main-jenkins-csb-crwqe.apps.ocp-c1.prod.psi.redhat.com/job/DS_CI/job/push-latest-container-to-quay_''' + JOB_BRANCH + '''/>pushed to 
    quay</a> and the <a href=https://github.com/redhat-developer/devspaces/actions/workflows/plugin-registry-build-publish-content-gh-pages.yaml>GH page update</a> for <a href=https://redhat-developer.github.io/devspaces/che-plugin-registry/next/x86_64/v3/plugins/>redhat-developer.github.io/devspaces/che-plugin-registry</a> (see <a href=https://github.com/redhat-developer/devspaces/commits/devspaces-3-rhel-8/dependencies/che-devfile-registry/webhook_trigger.txt>webhook_trigger.txt</a>)</li>
</ul></p>

<p>If <b style="color:green">downstream job fires</b>, see 
<a href=../sync-to-downstream_''' + JOB_BRANCH + '''/>sync-to-downstream</a>, then
<a href=../get-sources-rhpkg-container-build_''' + JOB_BRANCH + '''/>get-sources-rhpkg-container-build</a>. <br/>
   If <b style="color:orange">job is yellow</b>, no changes found to push, so no container-build triggered. </p>
<p>Results:<ul><li><a href=https://quay.io/devspaces/'''+MIDSTM_NAME+'''-rhel8>quay.io/devspaces/'''+MIDSTM_NAME+'''-rhel8</a></li></ul></p>
            ''')

            properties {
                githubProjectUrl("https://github.com/" + SOURCE_REPO)

                // only watch dependencies/UPSTM_NAME
                JobSharedUtils.enableDefaultPipelineWebhookTrigger(delegate, SOURCE_BRANCH, SOURCE_REPO, 
                    '$ref $files $name', 
                    'refs/heads/' + SOURCE_BRANCH + ' ' + 
                        '.*"dependencies/' + UPSTM_NAME + '/[^"]+?".*' + ' ' + 
                        SOURCE_REPO
                )

                disableResumeJobProperty()
            }

            quietPeriod(900) // limit builds to 1 every 15 mins (in sec)

            logRotator {
                daysToKeep(5)
                numToKeep(5)
                artifactDaysToKeep(2)
                artifactNumToKeep(1)
            }

            parameters{
                stringParam("SOURCE_REPO", SOURCE_REPO)
                stringParam("SOURCE_BRANCH", SOURCE_BRANCH)
                stringParam("MIDSTM_REPO", MIDSTM_REPO)
                stringParam("MIDSTM_BRANCH", MIDSTM_BRANCH)
                stringParam("MIDSTM_NAME", MIDSTM_NAME)
                booleanParam("FORCE_BUILD", false, "If true, trigger a rebuild even if no changes were pushed to pkgs.devel")
                booleanParam("CLEAN_ON_FAILURE", true, "If false, don't clean up workspace after the build so it can be used for debugging.")
            }

            definition {
                cps{
                    sandbox(true)
                    script(readFileFromWorkspace('jobs/DS_CI/template_'+JOB_BRANCH+'.jenkinsfile'))
                }
            }
        }
    }
}
