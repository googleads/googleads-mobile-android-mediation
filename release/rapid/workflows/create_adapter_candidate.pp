import '//releasetools/rapid/workflows/rapid.pp' as rapid

vars = rapid.create_vars() {}

final adapter_name = vars.process_arguments.get('adapter_name', [''])[0]
assert !is_null(adapter_name)

// kokoro job that rapid workflow will trigger for creating the adapter artifact.
// https://source.corp.google.com/piper///depot/google3/third_party/java_src/gma_sdk_mediation/kokoro/release.cfg
final kokoro_job = 'gma-sdk/android-mediation-adapters/release'
// Changelist at which rapid creates the release branch.
final changelist = '%(build_changelist)s'

// Description for the release cl created by rapid.
final cl_description = '#Maintenance - Updating #%s adapter for release version'

// The submit task in rapid takes all files from
// ${RAPID_PROCESS_DIR}/add_to_head and puts them in a CL.
final add_head = '$${RAPID_PROCESS_DIR}/add_to_head/google3/googledata/download/googleadmobadssdk/mediation/android/' + adapter_name

final tmp_dir = '%(working_dir)s/'+ adapter_name + '-release/'

final shell_mkdir_tmp = 'mkdir -p ' + tmp_dir

// copy all zip files to a dir under add_to_head
final shell_copy_cl_artifacts =
    'mkdir -p ' + add_head + ' && ' +
    'cp -r ' + tmp_dir + '*.zip ' + add_head

task_deps = [
  'source.integrate': ['start'],
  'piper.sync': ['source.integrate'],
  'changelog.integrate_log' : ['piper.sync'],
  'kokoro.trigger_build': ['changelog.integrate_log'],
  'shell-mkdir-tmp': ['kokoro.trigger_build'],
  'kokoro.fetch_artifact': ['shell-mkdir-tmp'],
  'shell-copy-cl-artifacts': ['kokoro.fetch_artifact'],
  'submit_files': ['shell-copy-cl-artifacts'],
]

task_properties = [
  'kokoro.trigger_build': [
    'full_job_name': kokoro_job,
    'build-params': 'ADAPTER_DIR=' + adapter_name,
    'wait_for_build': true,
    'multi_scm': true,
    'changelist': changelist,
    'use_overlay_branch': true,
    'kokoro_instance': 'prod',
  ],
  'shell-mkdir-tmp': [
    'command': shell_mkdir_tmp,
  ],
  'kokoro.fetch_artifact': [
    'full_job_name': kokoro_job,
    'kokoro_instance' : 'prod',
    'candidate_only' : true,
    'dest_dir': tmp_dir,
    'dest_overwrite_file': true,
    'dest_file_mode': '0o444',
  ],
  'shell-copy-cl-artifacts': [
    'command': shell_copy_cl_artifacts,
  ],
  'submit_files': [
    'submit_files_description': cl_description,
    'error_on_empty_cl': 'true',
    'submit_files_wait_for_review': 'true',
    # TODO(b/279778660) : Update the reviewer with oncall rotation.
    'submit_files_reviewer' : 'tukn',
    'autosubmit' : 'false'
  ],
],

workflow create_adapter_candidate = rapid.workflow([task_deps, task_properties]) {
  vars = @vars
}