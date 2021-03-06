import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import spock.lang.Ignore

public class EdgeXInfraShipLogsSpec extends JenkinsPipelineSpecification {

    def edgeXInfraShipLogs = null

    def setup() {

        edgeXInfraShipLogs = loadPipelineScriptForTest('vars/edgeXInfraShipLogs.groovy')
        explicitlyMockPipelineVariable('out')
    }

    def "Test edgeXInfraShipLogs [Should] throw exception [When] logSettingsFile is null" () {
        setup:
        when:
            edgeXInfraShipLogs({logSettingsFile = null})
        then:
            thrown Exception
    }

    def "Test edgeXInfraShipLogs [Should] call expected shell scripts with expected arguments [When] called" () {
        setup:
            def environmentVariables = [
                'DOCKER_REGISTRY': 'MyDockerRegistry',
                'ghprbPullId': true
            ]
            edgeXInfraShipLogs.getBinding().setVariable('env', environmentVariables)
            explicitlyMockPipelineStep('echo')
            explicitlyMockPipelineStep('withEnv')
            getPipelineMock('docker.image')('MyDockerRegistry:10003/edgex-lftools-log-publisher:alpine') >> explicitlyMockPipelineVariable('DockerImageMock')
            getPipelineMock("libraryResource")('global-jjb-shell/create-netrc.sh') >> {
                return 'create-netrc'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/logs-deploy.sh') >> {
                return 'logs-deploy'
            }
            getPipelineMock("libraryResource")('global-jjb-shell/logs-clear-credentials.sh') >> {
                return 'logs-clear-credentials'
            }
            edgeXInfraShipLogs.getBinding().setVariable('currentBuild', [:])
            edgeXInfraShipLogs.getBinding().setVariable('LOGS_SERVER', 'MyLogServer')
            edgeXInfraShipLogs.getBinding().setVariable('SILO', 'MySilo')
            edgeXInfraShipLogs.getBinding().setVariable('JENKINS_HOSTNAME', 'MyJenkinsHostname')
            edgeXInfraShipLogs.getBinding().setVariable('JOB_NAME', 'MyJobName')
            edgeXInfraShipLogs.getBinding().setVariable('BUILD_NUMBER', 'MyBuildNumber')
        when:
            edgeXInfraShipLogs()
        then:
            1 * getPipelineMock('DockerImageMock.inside').call(_) >> { _arguments ->
                def dockerArgs = '--privileged -u 0:0 -v /var/log/sa:/var/log/sa'
                assert dockerArgs == _arguments[0][0]
            }
            1 * getPipelineMock('withEnv').call(_) >> { _arguments ->
                def envArgs = [
                    'SERVER_ID=logs'
                ]
                assert envArgs == _arguments[0][0]
            }
            1 * getPipelineMock('sh').call([script:'create-netrc'])
            1 * getPipelineMock('sh').call([script:'logs-deploy'])
            1 * getPipelineMock('sh').call([script:'logs-clear-credentials'])
            // TODO: figure out how to check value of currentBuild.description mock variable
    }
}
