const execute = function (calc) {
  const i = calc.input;
  calc.log('邀请状态: ' + i.status + (i.status === 'valid' ? '（有效）' : '（无效）'));
  calc.log('手机号匹配: ' + (i.phoneMatches ? '是' : '否'));
  calc.log('账号匹配: ' + (i.accountMatches ? '是' : '否'));
  calc.log('工程匹配: ' + (i.projectMatches ? '是' : '否'));
  calc.log('单位匹配: ' + (i.unitMatches ? '是' : '否'));
  calc.log('角色匹配: ' + (i.roleMatches ? '是' : '否'));
  calc.log('重复请求: ' + (i.replay ? '是' : '否'));
  const allowed = i.status === 'valid' && i.phoneMatches && i.accountMatches && i.projectMatches && i.unitMatches && i.roleMatches && !i.replay;
  calc.log(allowed ? '决定: 接受邀请，新增一条成员关系和一条审计' : '决定: 拒绝邀请，不新增成员关系或审计');
  calc.output = {accept: allowed, membershipCreates: allowed ? 1 : 0, auditCreates: allowed ? 1 : 0};
};