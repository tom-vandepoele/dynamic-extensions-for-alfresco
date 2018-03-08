<!--
Changelog for DE

See http://keepachangelog.com/en as reference
Version template:

## [X.X.X] - yyyy-MM-dd
### Added (for new features)
### Changed (for changes in existing functionality)
### Deprecated (for soon-to-be removed features)
### Removed (for now removed features)
### Fixed (for any bug fixes)
### Security (in case of vulnerabilities)
### YANKED (for reverted functionality in)
 -->
# Dynamic Extensions For Alfresco Changelog

## [UNRELEASED]

## [1.7.4] - 2018-03-08
### Added
* [#173](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issue/173) Added CHANGELOG.md to the project

### Security
* [#176](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/pull/176) Fix Reflected XSS through maliciously formed URLs

## [1.7.3] - 2017-12-22
### Added
* [#167](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issue/167) Make WebScripts work with HttpEntity return value
* [#169](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issue/169) Add an issue and pull request template


## [1.7.2] - 2017-12-11
### Fixed
* [#156](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issue/156) NullPointerException in GenericQuartzJob
* [#160](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issue/160) Dynamic Extensions 1.7.1 fails to load on Alfresco 4.2.4


## [1.7.1] - 2017-10-16
### YANKED
* reverted `#145 Add lock support to scheduled jobs` due to NullPointerException that breaks DE.


## [1.7.0] - 2017-10-06 [YANKED]
### Added
* [#147](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issues/147) Add support for @ResponseBody annotation in webscript
* [#148](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issues/148) Add support for @RequestBody annotation in webscript

### Changed
* [#145](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/pull/145) Add lock support to scheduled jobs

### Fixed
* [#143](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issues/143) WebScriptUtil.extractHttpServletResponse() not working.
* [#151](https://github.com/xenit-eu/dynamic-extensions-for-alfresco/issues/151) @ResponseBody with void return type causes NullpointerException

